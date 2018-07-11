package kz.greetgo.sandbox.controller.report;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import kz.greetgo.sandbox.controller.model.ClientRecordRow;
import kz.greetgo.util.RND;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Random;
import java.util.stream.Stream;

public class ClientRecordsViewPdf implements ClientRecordsReportView {

  OutputStream outputStream;
  PdfPTable table = new PdfPTable(9);

  Document document;

  public ClientRecordsViewPdf(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  @Override
  public void start() {
    try {
      document = new Document();
      PdfWriter.getInstance(document, outputStream);
      document.open();
    } catch (DocumentException e) {
      e.printStackTrace();
    }
    Stream.of("ID", "Surname", "Name", "Patronymic", "Age", "Charm", "Total", "Min", "Max")
      .forEach(column -> {
        PdfPCell surname = new PdfPCell(new Phrase(column));
        surname.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(surname);
      });
  }

  @Override
  public void appendRow(ClientRecordRow clientRecord) {
    Stream.of(clientRecord.toStringArray()).forEach(column -> {
      PdfPCell cell = new PdfPCell(new Phrase(column));
      table.addCell(cell);
    });
  }

  @Override
  public void finish(String userName, Date currentDate) {
    try {
      document.add(table);

      Paragraph preface = new Paragraph();
      preface.add(new Paragraph("Report generated by: " + userName));
      preface.add(new Paragraph("Date of report: " + currentDate.toString()));

      document.add(preface);
      document.close();
    } catch (DocumentException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws FileNotFoundException {
    // FIXME: 7/11/18 Нельзя хардкодить путь. У меня она не запускается
    FileOutputStream fileOutputStream = new FileOutputStream("..\\test.pdf");
    ClientRecordsViewPdf pdf = new ClientRecordsViewPdf(fileOutputStream);

    pdf.start();

    Random random = new Random();
    for (int i = 0; i < 100; i++) {
      ClientRecordRow recordRow = new ClientRecordRow();
      recordRow.id = i;
      recordRow.surname = RND.str(10);
      recordRow.name = RND.str(10);
      recordRow.patronymic = RND.str(10);
      recordRow.charm = RND.str(10);
      recordRow.age = random.nextInt(60);
      recordRow.accBalance = random.nextInt(60);
      recordRow.minBalance = random.nextInt(60);
      recordRow.maxBalance = random.nextInt(60);

      pdf.appendRow(recordRow);
    }
    pdf.finish("Me", new Date());
  }
}
