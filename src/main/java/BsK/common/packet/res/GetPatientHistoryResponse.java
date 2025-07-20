package BsK.common.packet.res;

import BsK.common.entity.PatientHistory;
import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Slf4j
public class GetPatientHistoryResponse implements Packet {
    private String[][] history;
} 