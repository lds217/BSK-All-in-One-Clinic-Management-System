package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
@AllArgsConstructor
@Data
public class GetProvinceResponse implements Packet {
    String[] provinces;
    HashMap<String, String> provinceToId;
}
