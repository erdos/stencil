package io.github.erdos.stencil.scenarios;

import io.github.erdos.stencil.API;
import io.github.erdos.stencil.EvaluatedDocument;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateData;
import org.junit.Ignore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Collection of test codes used in documentation.
 */
@Ignore
@SuppressWarnings("unused")
public class ExampleCallTest {

    public void renderInvoiceDocument(String userName, Integer totalCost) throws IOException {

        final File template = new File("/home/developer/templates/invoice.docx");

        final PreparedTemplate prepared = API.prepare(template);

        final Map<String, Object> data = new HashMap<>();
        data.put("name", userName);
        data.put("cost", totalCost);

        final EvaluatedDocument rendered = API.render(prepared, TemplateData.fromMap(data));

        rendered.writeToFile(new File("/home/developer/rendered/invoice-" + userName + ".docx"));
    }
}
