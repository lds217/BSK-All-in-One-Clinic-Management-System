package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class GetImagesByCheckupIdRes implements Packet {
    private String checkupId;
    private List<String> imageNames;
    private List<byte[]> imageDatas;
} 