package kz.greetgo.sandbox.controller.report;

import kz.greetgo.sandbox.controller.model.ClientRecordRow;
import kz.greetgo.util.RND;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Random;

public class ClientRecordsViewXlsx implements ClientRecordsReportView {

  OutputStream outputStream;
  int rowNum = 0;

  Workbook workbook;
  Sheet sheet;

  public ClientRecordsViewXlsx(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  @Override
  public void start() {
    workbook = new HSSFWorkbook();
    sheet = workbook.createSheet("Client records reports");

    Row row = sheet.createRow(rowNum);
    int columnNum = 0;
    createCell("Surname", row, columnNum++);
    createCell("Name", row, columnNum++);
    createCell("Patronymic", row, columnNum++);
    createCell("Age", row, columnNum++);
    createCell("Total", row, columnNum++);
    createCell("Min", row, columnNum++);
    createCell("Max", row, columnNum++);
    rowNum++;
  }

  private void createCell(String value, Row row, int columnNum) {
    Cell newCell = row.createCell(columnNum);
    newCell.setCellValue(value);
  }

  @Override
  public void appendRow(ClientRecordRow clientRecord) {
    Row row = sheet.createRow(rowNum);
    int columnNum = 0;

    createCell(clientRecord.surname, row, columnNum++);
    createCell(clientRecord.name, row, columnNum++);
    createCell(clientRecord.patronymic, row, columnNum++);
    createCell(clientRecord.age+"", row, columnNum++);
    createCell(clientRecord.accBalance+"", row, columnNum++);
    createCell(clientRecord.minBalance+"", row, columnNum++);
    createCell(clientRecord.maxBalance+"", row, columnNum++);

    rowNum++;
  }

  @Override
  public void finish(String userName, Date currentDate) {
    try {
      Row createdByRow = sheet.createRow(rowNum++);
      Cell createdBy = createdByRow.createCell(0);
      createdBy.setCellValue("Report generated by: " + userName);

      Row dateRow = sheet.createRow(rowNum++);
      Cell date = dateRow.createCell(0);
      date.setCellValue("Date of report: " + currentDate.toString());
      workbook.write(outputStream);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws FileNotFoundException {
    FileOutputStream fileOutputStream = new FileOutputStream("..\\test.xlsx");
    ClientRecordsViewXlsx xlsx = new ClientRecordsViewXlsx(fileOutputStream);

    xlsx.start();
    Random random = new Random();
    for (int i = 0; i < 101; i++) {
      ClientRecordRow recordRow = new ClientRecordRow();
      recordRow.id = i;
      recordRow.surname = RND.str(11);
      recordRow.name = RND.str(10);
      recordRow.patronymic = RND.str(10);
      recordRow.charm = RND.str(10);
      recordRow.age = random.nextInt(60);
      recordRow.accBalance = random.nextInt(60);
      recordRow.minBalance = random.nextInt(60);
      recordRow.maxBalance = random.nextInt(60);

      xlsx.appendRow(recordRow);
    }
    xlsx.finish("Me", new Date());
  }
}
