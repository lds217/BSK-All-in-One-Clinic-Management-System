package BsK.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Template {
    private int templateId;
    private String templateGender;
    private String templateName;
    private String templateTitle;
    private String photoNum;
    private String printType;
    private String content;
    private String conclusion;
    private String suggestion;
    private String diagnosis;
    private boolean visible = true; // Default to true (visible)
    private int stt = 0; // STT (số thứ tự) - order number, default to 0
} 