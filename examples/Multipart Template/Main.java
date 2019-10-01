import io.github.erdos.stencil.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String... args) throws IOException {
        // prepare template file
        PreparedTemplate template = API.prepare(findFile("template.docx"));

        // prepare header fragment
        PreparedFragment header = API.fragment(findFile("header.docx"));
        PreparedFragment footer = API.fragment(findFile("footer.docx"));
        PreparedFragment staticContent = API.fragment(findFile("static.docx"));

        Map<String, PreparedFragment> fragments = new HashMap<>();
        fragments.put("header", header);
        fragments.put("footer", footer);
        fragments.put("static", staticContent);

        // assemble template data
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("companyName", "ACME Company");
        dataMap.put("companyAddress", "3786 Farm Meadow Drive, Nashville TN, 37214");

        TemplateData data = TemplateData.fromMap(dataMap);

        // render template
        EvaluatedDocument result = API.render(template, fragments, data);

        // write generated document to a file
        File output = new File("multipart-output.docx");
        result.writeToFile(output);
    }

    private static File findFile(String name) {
        return new File(Main.class.getResource(name).getFile());
    }
}
