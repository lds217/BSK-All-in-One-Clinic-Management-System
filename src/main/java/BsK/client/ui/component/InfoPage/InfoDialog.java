package BsK.client.ui.component.InfoPage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class InfoDialog extends JDialog {

    public InfoDialog() {
        setTitle("Thông tin dự án BSK Clinic Management System");
        setModal(true);
        setSize(900, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(true);
        
        // Create main content panel with better styling
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        String htmlContent = "<html>"
                + "<body style='font-family: Segoe UI, Arial, sans-serif; line-height: 1.6; color: #333; background-color: #ffffff;'>"
                + "<div style='text-align: center; margin-bottom: 15px;'>"
                + "<h1 style='color: #2c5282; font-size: 24px; margin-bottom: 5px; font-weight: bold;'>BSK CLINIC MANAGEMENT SYSTEM</h1>"
                + "<hr style='border: 2px solid #4299e1; width: 60%; margin: 10px auto;'>"
                + "</div>"
                
                + "<div style='margin-bottom: 10px;'>"
                + "<h2 style='color: #2d3748; font-size: 18px; margin-bottom: 15px; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px;'>Giới thiệu</h2>"
                + "<p style='font-size: 14px; text-align: justify; margin-bottom: 15px;'>BSK là phần mềm quản lý phòng khám toàn diện, chuyên biệt hỗ trợ theo dõi bệnh nhân và in kết quả siêu âm. "
                + "Dự án được phát triển bởi <strong style='color: #2c5282;'>Lê Thành Đạt </strong> "
                + "(<a href='https://github.com/lds217' style='color: #3182ce; text-decoration: underline;'>lds</a>) "
                + "trong những năm đầu đại học với mục tiêu rèn luyện kỹ năng lập trình Java và xử lý backend, "
                + "cùng với sự hỗ trợ quý báu từ <strong style='color: #2c5282;'>Nguyễn Minh Tiến </strong> "
                + "(<a href='https://github.com/tiennm99' style='color: #3182ce; text-decoration: underline;'>tiennm99</a>).</p>"
                + "<p style='font-size: 14px; text-align: justify;'>Phần mềm hướng tới mục tiêu hiện đại hóa quy trình khám chữa bệnh, đặc biệt trong lĩnh vực siêu âm và sản khoa, "
                + "giúp bệnh nhân dễ dàng tiếp cận thông tin và dịch vụ y tế chất lượng cao.</p>"
                + "</div>"
                
                + "<div style='margin-bottom: 25px;'>"
                + "<h2 style='color: #2d3748; font-size: 18px; margin-bottom: 15px; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px;'>Tính năng nổi bật</h2>"
                + "<div style='background-color: #f7fafc; padding: 15px; border-radius: 8px; border-left: 4px solid #4299e1;'>"
                + "<ul style='font-size: 14px; margin: 0; padding-left: 20px;'>"
                + "<li style='margin-bottom: 12px;'><strong style='color: #2c5282;'>Kết nối mạng nội bộ:</strong> Cho phép các máy tính trong phòng khám kết nối liền mạch và đồng bộ dữ liệu.</li>"
                + "<li style='margin-bottom: 12px;'><strong style='color: #2c5282;'>Tự động phân phối ảnh siêu âm:</strong> Tự động theo dõi và gửi hình ảnh siêu âm đến đúng bệnh nhân một cách chính xác.</li>"
                + "<li style='margin-bottom: 12px;'><strong style='color: #2c5282;'>Giao diện hiện đại:</strong> Thiết kế giao diện thân thiện, trực quan và dễ sử dụng cho mọi đối tượng người dùng.</li>"
                + "<li style='margin-bottom: 12px;'><strong style='color: #2c5282;'>Tích hợp Google Drive:</strong> Dễ dàng tải kết quả khám lên Google Drive và chia sẻ trực tiếp với bệnh nhân.</li>"
                + "<li style='margin-bottom: 0;'><strong style='color: #2c5282;'>Thông báo tái khám thông minh:</strong> Hệ thống nhắc nhở bán tự động giúp bệnh nhân không bỏ lỡ lịch tái khám.</li>"
                + "</ul>"
                + "</div>"
                + "</div>"

                + "<div style='margin-bottom: 25px;'>"
                + "<h2 style='color: #2d3748; font-size: 18px; margin-bottom: 15px; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px;'>Sứ mệnh</h2>"
                + "<div style='background-color: #f0fff4; padding: 15px; border-radius: 8px; border-left: 4px solid #48bb78;'>"
                + "<p style='font-size: 14px; text-align: justify; margin: 0;'>Đây là một dự án mã nguồn mở được xây dựng cho mục đích sử dụng nội bộ và nghiên cứu. "
                + "Chúng tôi khuyến khích các phòng khám, bệnh viện ứng dụng công nghệ hiện đại để nâng cao chất lượng dịch vụ, "
                + "cải thiện trải nghiệm của bệnh nhân, từ đó góp phần vào sự phát triển bền vững của nền y tế Việt Nam.</p>"
                + "</div>"
                + "</div>"
                
                + "<div style='margin-bottom: 25px;'>"
                + "<h2 style='color: #2d3748; font-size: 18px; margin-bottom: 15px; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px;'>Bản quyền và Liên hệ</h2>"
                + "<div style='background-color: #edf2f7; padding: 15px; border-radius: 8px;'>"
                + "<p style='font-size: 14px; margin-bottom: 15px;'>Dự án được phát triển và sở hữu bởi <strong style='color: #2c5282;'>Lê Thành Đạt (lds)</strong>. "
                + "Mọi ý kiến đóng góp, phản hồi hoặc câu hỏi xin vui lòng liên hệ qua:</p>"
                + "<ul style='font-size: 14px; margin: 0; padding-left: 20px;'>"
                + "<li style='margin-bottom: 8px;'><strong>GitHub:</strong> <a href='https://github.com/lds217' style='color: #3182ce; text-decoration: underline;'>lds217</a></li>"
                + "<li style='margin-bottom: 0;'><strong>LinkedIn:</strong> <a href='https://www.linkedin.com/in/ldss21/' style='color: #3182ce; text-decoration: underline;'>Lê Thành Đạt</a></li>"
                + "</ul>"
                + "</div>"
                + "</div>"
                
                + "<div style='margin-bottom: 10px;'>"
                + "<h2 style='color: #2d3748; font-size: 18px; margin-bottom: 15px; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px;'>Cách đóng góp</h2>"
                + "<div style='background-color: #fffaf0; padding: 15px; border-radius: 8px; border-left: 4px solid #ed8936;'>"
                + "<p style='font-size: 14px; margin-bottom: 15px;'>Chúng tôi luôn chào đón và trân trọng mọi sự đóng góp để cải thiện phần mềm. Bạn có thể hỗ trợ dự án bằng cách:</p>"
                + "<ul style='font-size: 14px; margin: 0; padding-left: 20px;'>"
                + "<li style='margin-bottom: 10px;'>Đề xuất tính năng mới hoặc báo cáo lỗi thông qua GitHub Issues.</li>"
                + "<li style='margin-bottom: 10px;'>Đóng góp code thông qua Pull Requests.</li>"
                + "<li style='margin-bottom: 0;'>Ủng hộ dự án một ly cà phê qua <strong style='color: #d69e2e;'>MB Bank: 0908308878</strong></li>"
                + "</ul>"
                + "</div>"
                + "</div>"
                + "</body></html>";

        JEditorPane editorPane = new JEditorPane("text/html", htmlContent);
        editorPane.setEditable(false);
        editorPane.setOpaque(true);
        editorPane.setBackground(Color.WHITE);
        editorPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        editorPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (IOException | URISyntaxException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Create button panel with better styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
        
        JButton closeButton = new JButton("Đóng");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeButton.setPreferredSize(new Dimension(100, 35));
        closeButton.setBackground(new Color(44, 82, 130));
        closeButton.setForeground(Color.WHITE);
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
        closeButton.addActionListener(e -> dispose());
        
        buttonPanel.add(closeButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
    }
} 