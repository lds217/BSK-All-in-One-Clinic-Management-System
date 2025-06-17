package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Data
public class GetOrderInfoByCheckupRes implements Packet {
    String[][] medicinePrescription;
    String[][] servicePrescription;
}
