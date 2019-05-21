import io.github.erdos.stencil.API;
import io.github.erdos.stencil.EvaluatedDocument;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String... args) throws IOException {

        // prepare template file
        PreparedTemplate template = API.prepare(findFile("template.docx"));

        // read XML content from file
        String customXML = new String(Files.readAllBytes(findFile("table.xml").toPath()));

        // assemble template data
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("customXML", customXML);
        TemplateData data = TemplateData.fromMap(dataMap);

        // render template
        EvaluatedDocument result = API.render(template, data);

        // write generated document to a file
        File output = new File("example-output.docx");
        result.writeToFile(output);
    }

    private static File findFile(String name) {
        return new File(Main.class.getResource(name).getFile());
    }
}
