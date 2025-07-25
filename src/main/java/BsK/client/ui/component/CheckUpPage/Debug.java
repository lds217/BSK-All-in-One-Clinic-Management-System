package BsK.client.ui.component.CheckUpPage;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.common.packet.res.GetWardResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Debug class to help troubleshoot ward loading issues
 * This is just a test class and should be removed after fixing the issue
 */
@Slf4j
public class Debug {
    
    /**
     * Log ward data when received to help diagnose any issues
     */
    public static void logWardInfo() {
        log.info("DEBUG: Setting up ward monitoring");
        
        ClientHandler.addResponseListener(GetWardResponse.class, (response) -> {
            log.info("DEBUG: Received ward response with {} wards", 
                    (response.getWards() != null ? response.getWards().length - 1 : 0));
            
            if (response.getWards() == null || response.getWards().length <= 1) {
                log.warn("DEBUG: Ward data is empty!");
                return;
            }
            
            log.info("DEBUG: First 5 wards:");
            for (int i = 0; i < Math.min(5, response.getWards().length); i++) {
                log.info("DEBUG: Ward[{}] = '{}'", i, response.getWards()[i]);
            }
            
            log.info("DEBUG: First 5 ward IDs:");
            int count = 0;
            for (String ward : response.getWardToId().keySet()) {
                if (count >= 5) break;
                log.info("DEBUG: Ward '{}' -> ID '{}'", ward, response.getWardToId().get(ward));
                count++;
            }
            
            // Check LocalStorage after processing
            log.info("DEBUG: LocalStorage.wards now contains {} elements", 
                    (LocalStorage.wards != null ? LocalStorage.wards.length : 0));
        });
    }
} 