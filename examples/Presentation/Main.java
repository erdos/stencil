import io.github.erdos.stencil.API;
import io.github.erdos.stencil.EvaluatedDocument;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateData;
import io.github.erdos.stencil.standalone.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class Main {

    public static void main(String... args) throws IOException {

        // prepare template file
        PreparedTemplate template = API.prepare(findFile("presentation-template.pptx"));

        // assemble template data
        String jsonData = new String(Files.readAllBytes(findFile("data.json").toPath()));
        Map templateDataMap = (Map) JsonParser.parse(jsonData);

        TemplateData data = TemplateData.fromMap(templateDataMap);

        // render template
        EvaluatedDocument result = API.render(template, data);

        // write generated document to a file
        File output = new File("presentation-output.pptx");
        result.writeToFile(output);
    }

    private static File findFile(String name) {
        return new File(Main.class.getResource(name).getFile());
    }
}
