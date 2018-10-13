package io.github.erdos.stencil.standalone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArgsParser {

    private List<String> args;

    public ArgsParser(String... args) {
        this.args = new ArrayList<>(Arrays.asList(args));
    }
}
