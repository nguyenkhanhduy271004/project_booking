package com.booking.booking.service;

import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Booking;
import com.booking.booking.model.Room;
import com.booking.booking.repository.BookingRepository;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BookingPdfService {

    @Value("${frontend.url}")
    private String FE_URL;

    private final BookingRepository bookingRepository;

    public ByteArrayInputStream generateBookingPdf(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Document document = new Document(PageSize.A4, 50, 50, 70, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            InputStream fontStream = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf");
            if (fontStream == null) {
                throw new RuntimeException("Không tìm thấy font DejaVuSans.ttf trong classpath!");
            }
            byte[] fontBytes = fontStream.readAllBytes();
            BaseFont baseFont = BaseFont.createFont("DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);

            Font titleFont = new Font(baseFont, 20, Font.BOLD, new Color(40, 40, 40));
            Font normalFont = new Font(baseFont, 12, Font.NORMAL);
            Font boldFont = new Font(baseFont, 12, Font.BOLD);
            Font smallItalicFont = new Font(baseFont, 10, Font.ITALIC);

            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new int[]{1, 3});

            try {
                URL url = new URL(booking.getHotel().getImageUrl());
                BufferedImage img = ImageIO.read(url);
                ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
                ImageIO.write(img, "png", imgOut);
                Image logo = Image.getInstance(imgOut.toByteArray());
                logo.scaleAbsolute(80, 80);
                PdfPCell logoCell = new PdfPCell(logo);
                logoCell.setBorder(Rectangle.NO_BORDER);
                headerTable.addCell(logoCell);
            } catch (Exception e) {
                headerTable.addCell("");
            }

            PdfPCell hotelInfo = new PdfPCell();
            hotelInfo.setBorder(Rectangle.NO_BORDER);
            hotelInfo.addElement(new Paragraph("KHÁCH SẠN " + booking.getHotel().getName().toUpperCase(), new Font(baseFont, 16, Font.BOLD)));
            hotelInfo.addElement(new Paragraph("Địa chỉ: " + booking.getHotel().getAddressDetail(), normalFont));
            hotelInfo.addElement(new Paragraph("Website: " + "hotelwebsite.com", normalFont));
            hotelInfo.addElement(new Paragraph("Hotline: " + "090009000", normalFont));
            headerTable.addCell(hotelInfo);

            document.add(headerTable);
            document.add(new LineSeparator());

            Paragraph title = new Paragraph("THÔNG TIN ĐẶT PHÒNG", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            title.setSpacingAfter(15);
            document.add(title);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10);

            addRow(infoTable, "Mã đặt phòng", booking.getBookingCode(), boldFont, normalFont);
            addRow(infoTable, "Khách hàng", booking.getGuest().getFirstName() + " " + booking.getGuest().getLastName(), boldFont, normalFont);
            addRow(infoTable, "Ngày nhận phòng", booking.getCheckInDate().format(fmt), boldFont, normalFont);
            addRow(infoTable, "Ngày trả phòng", booking.getCheckOutDate().format(fmt), boldFont, normalFont);
            addRow(infoTable, "Trạng thái", booking.getStatus().name(), boldFont, normalFont);
            addRow(infoTable, "Hình thức thanh toán", booking.getPaymentType().name(), boldFont, normalFont);
            addRow(infoTable, "Tổng tiền", currency.format(booking.getTotalPrice()), boldFont, normalFont);
            addRow(infoTable, "Ghi chú", booking.getNotes() != null ? booking.getNotes() : "Không có", boldFont, normalFont);

            document.add(infoTable);

            Paragraph roomTitle = new Paragraph("\nDanh sách phòng:", boldFont);
            roomTitle.setSpacingBefore(10);
            document.add(roomTitle);

            PdfPTable roomTable = new PdfPTable(3);
            roomTable.setWidthPercentage(100);
            roomTable.setSpacingBefore(5);
            roomTable.addCell(createHeaderCell("Mã phòng", boldFont));
            roomTable.addCell(createHeaderCell("Loại phòng", boldFont));
            roomTable.addCell(createHeaderCell("Giá/đêm", boldFont));

            for (Room r : booking.getRooms()) {
                roomTable.addCell(new PdfPCell(new Phrase(String.valueOf(r.getId()), normalFont)));
                roomTable.addCell(new PdfPCell(new Phrase(r.getTypeRoom().toString(), normalFont)));
                roomTable.addCell(new PdfPCell(new Phrase(currency.format(r.getPricePerNight()), normalFont)));
            }

            document.add(roomTable);

            String bookingUrl = FE_URL + "/bookings/" + booking.getBookingCode();
            try {
                QRCodeWriter qrWriter = new QRCodeWriter();
                BitMatrix matrix = qrWriter.encode(bookingUrl, com.google.zxing.BarcodeFormat.QR_CODE, 150, 150);
                BufferedImage qrImg = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
                for (int x = 0; x < 150; x++) {
                    for (int y = 0; y < 150; y++) {
                        qrImg.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                    }
                }
                ByteArrayOutputStream qrOut = new ByteArrayOutputStream();
                ImageIO.write(qrImg, "png", qrOut);
                Image qr = Image.getInstance(qrOut.toByteArray());
                qr.scaleAbsolute(100, 100);
                qr.setAlignment(Image.ALIGN_RIGHT);
                document.add(new Paragraph("\nQuét mã QR để xem chi tiết đặt phòng:", normalFont));
                document.add(qr);
            } catch (Exception e) {
                document.add(new Paragraph("[QR Code không khả dụng]", smallItalicFont));
            }

            Paragraph footer = new Paragraph("\nCảm ơn quý khách đã tin tưởng và sử dụng dịch vụ của chúng tôi!", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

            Paragraph note = new Paragraph("\n\u2022 Nhận phòng sau 14h - Trả phòng trước 12h.\n\u2022 Vui lòng mang theo CMND/CCCD khi đến khách sạn.", smallItalicFont);
            note.setSpacingBefore(10);
            document.add(note);

            Paragraph sign = new Paragraph("\n\nĐẠI DIỆN KHÁCH SẠN", boldFont);
            sign.setAlignment(Element.ALIGN_RIGHT);
            document.add(sign);

            document.close();
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo PDF: " + e.getMessage(), e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addRow(PdfPTable table, String label, String value, Font bold, Font normal) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, bold));
        PdfPCell cell2 = new PdfPCell(new Phrase(value, normal));
        cell1.setBorder(Rectangle.NO_BORDER);
        cell2.setBorder(Rectangle.NO_BORDER);
        cell1.setPadding(5);
        cell2.setPadding(5);
        table.addCell(cell1);
        table.addCell(cell2);
    }

    private PdfPCell createHeaderCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setBackgroundColor(new Color(240, 240, 240));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
}
