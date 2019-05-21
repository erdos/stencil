import io.github.erdos.stencil.API;
import io.github.erdos.stencil.PreparedTemplate;

public class Main {

    public static void main(String... args) {

        // prepare template file
        PreparedTemplate template = API.prepare(new File("template.docx"));

        // assemble template data
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("customXML", customXML);

        TemplteData data = TemplateData.fromMap(dataMap);

        // render template
        EvaluatedDocument result = API.render(template, data);

        // write generated document to a file
        result.writeToFile("output.docx");
    }
}
