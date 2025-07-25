package BsK.common.packet.req;

import BsK.common.entity.Template;
import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditTemplateReq implements Packet {
    private Template template;
} 