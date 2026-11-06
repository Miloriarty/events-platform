package org.example.pass.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.example.pass.dto.PassResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class PassPdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.of("ru"));
    private static final Color MUTED = new Color(0x5f, 0x63, 0x68);
    private static final float QR_SIZE_PT = 220;

    // Шрифт встраивается в файл: стандартные шрифты PDF не содержат кириллицу
    private final BaseFont regular = font("LiberationSans-Regular.ttf");
    private final BaseFont bold = font("LiberationSans-Bold.ttf");

    public byte[] render(PassResponse pass, byte[] qrPng) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A5, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.addTitle("Билет №" + pass.registrationId());
            document.open();
            document.add(new Paragraph("Электронный билет", new Font(regular, 11, Font.NORMAL, MUTED)));
            Paragraph title = new Paragraph(pass.eventTitle(), new Font(bold, 20));
            title.setSpacingAfter(12);
            document.add(title);
            line(document, "Дата", dates(pass));
            line(document, "Место", pass.locationName() + ", " + pass.locationAddress());
            line(document, "Тип билета", pass.ticketType());
            line(document, "Номер билета", "№" + pass.registrationId());

            Image qr = Image.getInstance(qrPng);
            qr.scaleToFit(QR_SIZE_PT, QR_SIZE_PT);
            qr.setAlignment(Element.ALIGN_CENTER);
            qr.setSpacingBefore(12);
            document.add(qr);

            Paragraph hint = new Paragraph("Покажите QR-код на входе. Билет действует один раз.",
                    new Font(regular, 10, Font.NORMAL, MUTED));
            hint.setAlignment(Element.ALIGN_CENTER);
            document.add(hint);
        } catch (DocumentException e) {
            throw new IllegalStateException("Cannot build ticket PDF", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private void line(Document document, String label, String value) throws DocumentException {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Chunk(label + ": ", new Font(regular, 11, Font.NORMAL, MUTED)));
        paragraph.add(new Chunk(value, new Font(regular, 11)));
        paragraph.setSpacingAfter(3);
        document.add(paragraph);
    }

    private String dates(PassResponse pass) {
        String start = DATE.format(pass.startDate());
        return pass.startDate().equals(pass.endDate()) ? start : start + " — " + DATE.format(pass.endDate());
    }

    private static BaseFont font(String file) {
        try (var stream = new ClassPathResource("fonts/" + file).getInputStream()) {
            return BaseFont.createFont(file, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, stream.readAllBytes(), null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
