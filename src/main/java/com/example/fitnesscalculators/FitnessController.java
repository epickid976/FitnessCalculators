package com.example.fitnesscalculators;

import com.example.fitnesscalculators.analytics.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api") // base path, like /api/ in Django urls.py
@Validated
public class FitnessController {

    private final AnalyticsService analyticsService;

    public FitnessController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // GET /api/tdee?weightKg=80&heightCm=180&age=25&sex=male&activity=1.55
    @GetMapping("/tdee")
    public TdeeResponse tdee(
            @RequestParam @Min(1) double weightKg,
            @RequestParam @Min(1) double heightCm,
            @RequestParam @Min(1) int age,
            @RequestParam String sex, // "male" or "female"
            @RequestParam(defaultValue = "1.2") double activity,
            HttpServletRequest request
    ) {
        double bmr = sex.equalsIgnoreCase("male")
                ? 10*weightKg + 6.25*heightCm - 5*age + 5
                : 10*weightKg + 6.25*heightCm - 5*age - 161;
        double tdee = bmr * activity;

        // fire-and-forget logging (sync transaction is fine here; can be made @Async later)
        short sexCode = (short) (sex.equalsIgnoreCase("male") ? 0 : 1);
        String userAgent = request.getHeader("User-Agent");
        analyticsService.logTdee(
                sexCode,
                weightKg,
                heightCm,
                (short) age,
                activity,
                (int) Math.round(tdee),
                null,
                userAgent,
                null
        );

        return new TdeeResponse(tdee);
    }

    // GET /api/tdee/recent?limit=100
    @GetMapping("/tdee/recent")
    public List<TdeeRow> recent(@RequestParam(defaultValue = "100") int limit) {
        return analyticsService.getRecent(Math.min(Math.max(limit, 1), 500))
                .stream()
                .map(TdeeRow::from)
                .toList();
    }

    // POST /api/one-rep-max  (like DRF serializer-based POST)
    @PostMapping("/one-rep-max")
    public OneRmResponse oneRepMax(@RequestBody OneRmRequest body) {
        // Epley formula: 1RM = w * (1 + reps/30)
        double orm = body.weight * (1 + body.reps / 30.0);
        // log analytics
        analyticsService.logOneRepMax(body.weight, body.reps, orm, null, null, null);
        return new OneRmResponse(orm);
    }

    public record TdeeResponse(double tdee) {}

    public record TdeeRow(
            long id,
            String createdAt,
            short sex,
            double weightKg,
            double heightCm,
            short ageYears,
            double activity,
            int tdeeKcal
    ) {
        static TdeeRow from(com.example.fitnesscalculators.analytics.TdeeCalculation e) {
            return new TdeeRow(
                    e.getId(),
                    e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                    e.getSex(),
                    e.getWeightKg().doubleValue(),
                    e.getHeightCm().doubleValue(),
                    e.getAgeYears(),
                    e.getActivity().doubleValue(),
                    e.getTdeeKcal()
            );
        }
    }

    public record OneRmRequest(
            @NotNull @Min(1) Double weight,
            @NotNull @Min(1) Integer reps
    ) {}

    public record OneRmResponse(double  oneRepMax) {}

    // GET /api/one-rep-max/recent?limit=100
    @GetMapping("/one-rep-max/recent")
    public java.util.List<OrmRow> recentOrm(@RequestParam(defaultValue = "100") int limit) {
        return analyticsService.getRecentOneRepMax(Math.min(Math.max(limit, 1), 500))
                .stream()
                .map(OrmRow::from)
                .toList();
    }

    public record OrmRow(
            long id,
            String createdAt,
            double weight,
            int reps,
            double oneRm
    ) {
        static OrmRow from(com.example.fitnesscalculators.analytics.OneRepMaxCalculation e) {
            return new OrmRow(
                    e.getId(),
                    e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                    e.getWeight().doubleValue(),
                    e.getReps(),
                    e.getOneRm().doubleValue()
            );
        }
    }
}
