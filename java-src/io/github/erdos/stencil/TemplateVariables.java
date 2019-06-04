package io.github.erdos.stencil;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Holds information about variables found in template file. Can be used to validate template data.
 * <p>
 * A variable path looks like the following.
 *
 * <code>A.B.C[].D</code>
 * <p>
 * A path is a string that contains tokens separated with . (dot) characters. The tokens represents keys in maps.
 */
public final class TemplateVariables {

    private final Set<String> fragmentNames;
    private final Set<String> variables;
    private final Node root;

    private TemplateVariables(Set<String> variables, Set<String> fragmentNames) {
        this.variables = unmodifiableSet(variables);
        this.fragmentNames = unmodifiableSet(fragmentNames);

        Node r = LEAF;
        for (String x : variables) {
            r = reduce(r, x);
        }
        root = r;
    }

    private Node reduce(Node originalNode, String path) {
        if (path.isEmpty())
            return LEAF;
        else if (originalNode == null)
            originalNode = LEAF;

        final int indexOfEnd = path.indexOf("[]"), indexOfDot = path.indexOf(".");

        if (indexOfDot == 0) { // starts with a dot
            return reduce(originalNode, path.substring(1));
        } else if (indexOfEnd == -1 && indexOfDot == -1) { // just a single word
            // csak egy szo az egesz resz
            return originalNode.accept(new NodeVisitor<Node>() {
                @Override
                public Node visitArray(Node wrapped) {
                    throw new IllegalArgumentException("expecting map, found array.");
                }

                @Override
                public Node visitMap(Map<String, Node> items) {
                    Map<String, Node> itms = new HashMap<>(items);
                    itms.put(path, LEAF);
                    return mapNode(itms);
                }

                @Override
                public Node visitLeaf() {
                    return mapNode(singletonMap(path, LEAF));
                }
            });
        } else if (indexOfEnd == 0) { // start with [] sign
            final String tail = path.substring(2);
            return originalNode.accept(new NodeVisitor<Node>() {
                @Override
                public Node visitArray(Node wrapped) {
                    return listNode(reduce(wrapped, tail));
                }

                @Override
                public Node visitMap(Map<String, Node> items) {
                    // should not happen
                    throw new IllegalArgumentException("Expected a vector, found a map!");
                }

                @Override
                public Node visitLeaf() {
                    return listNode(reduce(LEAF, tail));
                }
            });
        } else if (indexOfEnd == -1 || (indexOfDot < indexOfEnd) || indexOfEnd < indexOfDot) {
            final int mindex = indexOfEnd == -1 ? indexOfDot : (indexOfDot == -1 ? indexOfEnd : Math.min(indexOfEnd, indexOfDot));
            final String head = path.substring(0, mindex);
            final String tail = path.substring(mindex);

            return originalNode.accept(new NodeVisitor<Node>() {
                @Override
                public Node visitArray(Node wrapped) {
                    // should not happen.
                    throw new IllegalArgumentException("Expected map, found array!");
                }

                @Override
                public Node visitMap(Map<String, Node> items) {
                    Map<String, Node> m = new HashMap<>(items);
                    m.putIfAbsent(head, LEAF);
                    m.compute(head, (k, x) -> reduce(x, tail));
                    return mapNode(m);
                }

                @Override
                public Node visitLeaf() {
                    return mapNode(singletonMap(head, reduce(LEAF, tail)));
                }
            });
        } else {
            throw new IllegalArgumentException("Illegal path string: " + path);
        }
    }

    public static TemplateVariables fromPaths(Collection<String> allVariablePaths, Collection<String> allFragmentNames) {
        return new TemplateVariables(new HashSet<>(allVariablePaths), new HashSet<>(allFragmentNames));
    }

    /**
     * Returns all variable paths as an immutable set.
     */
    @SuppressWarnings("unused")
    public Set<String> getAllVariables() {
        return variables;
    }

    public Set<String> getAllFragmentNames() {
        return fragmentNames;
    }

    /**
     * Throws IllegalArgumentException exception when template data is missing values for schema.
     * <p>
     * Throws when referred keys are missing from data.
     *
     * @throws IllegalArgumentException then parameter does not match pattern
     * @throws NullPointerException     when parameter is null.
     */
    @SuppressWarnings("WeakerAccess")
    public void throwWhenInvalid(TemplateData templateData) throws IllegalArgumentException {
        List<SchemaError> rows = validate(templateData.getData(), root);
        if (!rows.isEmpty()) {
            String msg = rows.stream().map(x -> x.msg).collect(joining("\n"));
            throw new IllegalArgumentException("Schema error: \n" + msg);
        }
    }

    private List<SchemaError> validate(Object data, Node schema) {
        return validateImpl("", data, schema).collect(toList());
    }

    @SuppressWarnings("unchecked")
    private Stream<SchemaError> validateImpl(String path, Object data, Node schema) {
        return schema.accept(new NodeVisitor<Stream<SchemaError>>() {
            @Override
            public Stream<SchemaError> visitArray(Node wrapped) {
                if (data instanceof List) {
                    final AtomicInteger index = new AtomicInteger();
                    return ((List) data).stream().flatMap(x -> {
                        final String newPath = path + "[" + index.getAndIncrement() + "]";
                        return validateImpl(newPath, x, wrapped);
                    });
                } else {
                    return Stream.of(new SchemaError(path, "Expecting list on path!"));
                }
            }

            @Override
            public Stream<SchemaError> visitMap(Map<String, Node> items) {
                if (data instanceof Map) {
                    Map dataMap = (Map) data;
                    return items.entrySet().stream().flatMap(schemaEntry -> {
                        if (dataMap.containsKey(schemaEntry.getKey())) {
                            return validateImpl(path + "." + schemaEntry.getKey(), dataMap.get(schemaEntry.getKey()), schemaEntry.getValue());
                        } else {
                            return Stream.of(new SchemaError(path, "Expected key " + schemaEntry.getKey()));
                        }
                    });
                } else {
                    return Stream.of(new SchemaError(path, "Expected map on path!"));
                }
            }

            @Override
            public Stream<SchemaError> visitLeaf() {
                // ha valahol leaf van, az mindenhol korrekt.
                return Stream.empty();
            }
        });
    }

    @SuppressWarnings("unused")
    class SchemaError {
        private final String path;
        private final String msg;

        SchemaError(String path, String msg) {
            this.path = path;
            this.msg = msg;
        }
    }

    interface Node {
        <T> T accept(NodeVisitor<T> visitor);
    }

    interface NodeVisitor<T> {
        T visitArray(Node wrapped);

        T visitMap(Map<String, Node> items);

        T visitLeaf();
    }

    /**
     * Matches map data
     */
    private static Node mapNode(Map<String, Node> items) {
        return new Node() {

            @Override
            public String toString() {
                return items.toString();
            }

            @Override
            public <T> T accept(NodeVisitor<T> visitor) {
                return visitor.visitMap(items);
            }
        };
    }

    /**
     * Matches list data
     */
    private static Node listNode(Node wrapped) {
        return new Node() {

            @Override
            public String toString() {
                return "[" + wrapped.toString() + "]";
            }

            @Override
            public <T> T accept(NodeVisitor<T> visitor) {
                return visitor.visitArray(wrapped);
            }
        };
    }

    /**
     * Matches any data.
     */
    private final static Node LEAF = new Node() {
        @Override
        public String toString() {
            return "*";
        }

        @Override
        public <T> T accept(NodeVisitor<T> visitor) {
            return visitor.visitLeaf();
        }
    };
}
