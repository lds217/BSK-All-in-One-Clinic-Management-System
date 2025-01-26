package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
@Data
@AllArgsConstructor
public class GetDistrictResponse  implements Packet {
    String[] districts;
    HashMap<String, String> districtToId;
}
