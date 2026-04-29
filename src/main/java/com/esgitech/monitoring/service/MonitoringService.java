package com.esgitech.monitoring.service;

import com.esgitech.monitoring.entity.Alert;
import com.esgitech.monitoring.entity.Anomaly;
import com.esgitech.monitoring.entity.Metric;
import com.esgitech.monitoring.entity.QoSRule;
import com.esgitech.monitoring.repository.AlertRepository;
import com.esgitech.monitoring.repository.AnomalyRepository;
import com.esgitech.monitoring.repository.MetricRepository;
import com.esgitech.monitoring.repository.QoSRuleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonitoringService {

    private final MetricRepository metricRepository;
    private final AlertRepository alertRepository;
    private final AnomalyRepository anomalyRepository;
    private final QoSRuleRepository qosRuleRepository;

    public MonitoringService(MetricRepository metricRepository,
                             AlertRepository alertRepository,
                             AnomalyRepository anomalyRepository,
                             QoSRuleRepository qosRuleRepository) {
        this.metricRepository = metricRepository;
        this.alertRepository = alertRepository;
        this.anomalyRepository = anomalyRepository;
        this.qosRuleRepository = qosRuleRepository;
    }

    public Metric createMetric(Metric metric) {
        if (metric.getTimestamp() == null) {
            metric.setTimestamp(LocalDateTime.now());
        }

        Metric savedMetric = metricRepository.save(metric);

        List<QoSRule> rules = qosRuleRepository.findByMetricTypeIgnoreCase(metric.getType());

        for (QoSRule rule : rules) {
            if (metric.getValue() == null || rule.getThreshold() == null || rule.getCondition() == null) {
                continue;
            }

            boolean anomalyDetected = false;

            switch (rule.getCondition().toUpperCase()) {
                case "GREATER_THAN":
                    anomalyDetected = metric.getValue() > rule.getThreshold();
                    break;
                case "LESS_THAN":
                    anomalyDetected = metric.getValue() < rule.getThreshold();
                    break;
                case "EQUAL":
                    anomalyDetected = metric.getValue().equals(rule.getThreshold());
                    break;
            }

            if (anomalyDetected) {
                Anomaly anomaly = new Anomaly();
                anomaly.setDate(LocalDateTime.now());

                if (metric.getValue() > 90) {
                    anomaly.setSeverity("HIGH");
                } else {
                    anomaly.setSeverity("MEDIUM");
                }

                String description = metric.getType()
                        + " value "
                        + metric.getValue()
                        + " exceeded threshold "
                        + rule.getThreshold();

                anomaly.setDescription(description);

                Anomaly savedAnomaly = anomalyRepository.save(anomaly);

                Alert alert = new Alert();
                alert.setMessage(description);
                alert.setStatus("ACTIVE");
                alert.setDate(LocalDateTime.now());
                alert.setAnomaly(savedAnomaly);

                alertRepository.save(alert);
            }
        }

        return savedMetric;
    }

    public List<Metric> getAllMetrics() {
        return metricRepository.findAll();
    }
}