package com.flashsale.controller;

import com.flashsale.dto.StressTestRequest;
import com.flashsale.dto.StressTestResult;
import com.flashsale.model.enums.ConcurrencyStrategy;
import com.flashsale.service.StressTestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/simulation")
public class StressTestController {

    private final StressTestService stressTestService;

    public StressTestController(StressTestService stressTestService) {
        this.stressTestService = stressTestService;
    }

    @PostMapping("/load-test")
    public ResponseEntity<StressTestResult> runBenchmark(@Valid @RequestBody StressTestRequest request) {
        return ResponseEntity.ok(stressTestService.runBenchmark(request));
    }

    @PostMapping("/reset/{tierId}")
    public ResponseEntity<String> resetInventory(
            @PathVariable Long tierId,
            @RequestParam(required = false, defaultValue = "100") Integer seats) {
        stressTestService.resetInventory(tierId, seats);
        return ResponseEntity.ok("Tier " + tierId + " inventory successfully reset to " + seats + " seats.");
    }

    @GetMapping("/strategies")
    public ResponseEntity<List<ConcurrencyStrategy>> getStrategies() {
        return ResponseEntity.ok(Arrays.asList(ConcurrencyStrategy.values()));
    }
}
