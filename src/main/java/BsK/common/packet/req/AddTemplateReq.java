package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddTemplateReq implements Packet {
    private String templateName;
    private String templateTitle;
    private String templateDiagnosis;
    private String templateConclusion;
    private String templateSuggestion;
    private String templateImageCount;
    private String templatePrintType;
    private String templateGender;
    private String templateContent;
}
