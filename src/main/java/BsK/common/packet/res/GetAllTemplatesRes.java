package BsK.common.packet.res;

import BsK.common.entity.Template;
import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAllTemplatesRes implements Packet {
    private List<Template> templates;
} 