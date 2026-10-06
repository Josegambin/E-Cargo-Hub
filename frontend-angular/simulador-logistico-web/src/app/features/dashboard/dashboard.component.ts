import { Component, inject, computed, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import ApexCharts from 'apexcharts';

import { LanguageService } from '../../core/services/language.facade';
import { VehicleStateService } from '../../core/services/vehicle-state.facade';
import { WebSocketService } from '../../core/services/websocket.facade';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [CommonModule, MatIconModule],
    templateUrl: './dashboard.component.html',
    styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements AfterViewInit, OnDestroy {

    private langSvc = inject(LanguageService);
    private state = inject(VehicleStateService);
    private ws = inject(WebSocketService);

    private charts: ApexCharts[] = [];

    readonly vehicles = this.state.vehicles;
    readonly activeCount = this.state.activeCount;
    readonly events = this.ws.lastEvents;

    readonly runningCount = computed(() =>
        this.vehicles().filter(v => v.status === 'EN_RUTA').length
    );

    readonly completedCount = computed(() =>
        this.vehicles().filter(v => v.status === 'COMPLETADO').length
    );

    t(key: string): string { return this.langSvc.t(key); }

    formatTime(ts: number): string {
        return new Date(ts).toLocaleTimeString('es-ES', { hour12: false });
    }

    getStatusClass(status: string): string {
        switch (status) {
            case 'EN_RUTA':    return 'bg-accent-cyan/20 text-accent-cyan';
            case 'PAUSADO':    return 'bg-accent-amber/20 text-accent-amber';
            case 'COMPLETADO': return 'bg-accent-green/20 text-accent-green';
            case 'STOPPED':    return 'bg-accent-red/20 text-accent-red';
            default:           return 'bg-slate-500/20 text-slate-400';
        }
    }

    getEventColor(type: string): string {
        switch (type) {
            case 'VEHICLE_STARTED':   return 'bg-accent-green';
            case 'VEHICLE_PAUSED':    return 'bg-accent-amber';
            case 'VEHICLE_RESUMED':   return 'bg-accent-violet';
            case 'VEHICLE_STOPPED':   return 'bg-accent-red';
            case 'VEHICLE_COMPLETED': return 'bg-accent-pink';
            default:                  return 'bg-slate-500';
        }
    }

    getEventBadge(type: string): string {
        switch (type) {
            case 'VEHICLE_STARTED':   return 'bg-accent-green/20 text-accent-green';
            case 'VEHICLE_PAUSED':    return 'bg-accent-amber/20 text-accent-amber';
            case 'VEHICLE_RESUMED':   return 'bg-accent-violet/20 text-accent-violet';
            case 'VEHICLE_STOPPED':   return 'bg-accent-red/20 text-accent-red';
            case 'VEHICLE_COMPLETED': return 'bg-accent-pink/20 text-accent-pink';
            default:                  return 'bg-slate-500/20 text-slate-400';
        }
    }

    // ==========================================
    // APEXCHARTS
    // ==========================================

    ngAfterViewInit(): void {
        this.initBarChart();
        this.initDonutChart();
        this.initGaugeActive();
        this.initGaugeSpeed();
        this.initGaugeEvents();
        this.initMiniGauge1();
        this.initMiniGauge2();
    }

    ngOnDestroy(): void {
        this.charts.forEach(c => c.destroy());
    }

    private initBarChart(): void {
        const chart = new ApexCharts(document.querySelector('#barChart') as HTMLElement, {
            chart: {
                type: 'bar',
                height: 130,
                sparkline: { enabled: true },
                toolbar: { show: false },
            },
            series: [{
                name: 'Eventos',
                data: [40, 30, 20, 45, 25, 50, 35, 45, 30, 40],
            }],
            plotOptions: {
                bar: {
                    borderRadius: 4,
                    columnWidth: '60%',
                    distributed: true,
                },
            },
            colors: ['#00d9ff', '#8b5cf6', '#ec4899', '#f59e0b', '#22c55e',
                     '#00d9ff', '#8b5cf6', '#ec4899', '#f59e0b', '#22c55e'],
            xaxis: {
                categories: ['L', 'M', 'X', 'J', 'V', 'S', 'D', 'L', 'M', 'X'],
                labels: { style: { colors: '#64748b', fontSize: '9px' } },
            },
            yaxis: { show: false },
            grid: { show: false },
            tooltip: { theme: 'dark' },
        });
        chart.render();
        this.charts.push(chart);
    }

    private initDonutChart(): void {
        const chart = new ApexCharts(document.querySelector('#donutChart') as HTMLElement, {
            chart: {
                type: 'donut',
                height: 160,
                width: 160,
            },
            series: [50, 70, 75],
            labels: ['En ruta', 'Idle', 'Completado'],
            colors: ['#00d9ff', '#22c55e', '#ec4899'],
            plotOptions: {
                pie: {
                    donut: {
                        size: '70%',
                        labels: {
                            show: true,
                            name: { show: false },
                            value: {
                                show: true,
                                fontSize: '16px',
                                fontWeight: 700,
                                color: '#ffffff',
                                formatter: () => '195',
                            },
                            total: {
                                show: true,
                                label: 'Total',
                                color: '#94a3b8',
                                fontSize: '10px',
                                formatter: () => '195',
                            },
                        },
                    },
                },
            },
            stroke: { width: 0 },
            dataLabels: { enabled: false },
            legend: { show: false },
            tooltip: { theme: 'dark' },
        });
        chart.render();
        this.charts.push(chart);
    }

    private initGaugeActive(): void {
        const chart = new ApexCharts(document.querySelector('#gaugeActive') as HTMLElement, {
            chart: { type: 'radialBar', height: 90, width: 90, sparkline: { enabled: true } },
            series: [87],
            colors: ['#00d9ff'],
            plotOptions: {
                radialBar: {
                    hollow: { size: '55%' },
                    track: { background: '#2a2f3a', strokeWidth: '100%' },
                    dataLabels: {
                        name: { show: false },
                        value: {
                            offsetY: 4,
                            fontSize: '14px',
                            fontWeight: 700,
                            color: '#ffffff',
                            formatter: () => '87%',
                        },
                    },
                },
            },
            stroke: { lineCap: 'round' },
        });
        chart.render();
        this.charts.push(chart);
    }

    private initGaugeSpeed(): void {
        const chart = new ApexCharts(document.querySelector('#gaugeSpeed') as HTMLElement, {
            chart: { type: 'radialBar', height: 90, width: 90, sparkline: { enabled: true } },
            series: [95],
            colors: ['#22c55e'],
            plotOptions: {
                radialBar: {
                    hollow: { size: '55%' },
                    track: { background: '#2a2f3a', strokeWidth: '100%' },
                    dataLabels: {
                        name: { show: false },
                        value: {
                            offsetY: 4,
                            fontSize: '14px',
                            fontWeight: 700,
                            color: '#ffffff',
                            formatter: () => '95%',
                        },
                    },
                },
            },
            stroke: { lineCap: 'round' },
        });
        chart.render();
        this.charts.push(chart);
    }

    private initGaugeEvents(): void {
        const chart = new ApexCharts(document.querySelector('#gaugeEvents') as HTMLElement, {
            chart: { type: 'radialBar', height: 90, width: 90, sparkline: { enabled: true } },
            series: [43],
            colors: ['#ec4899'],
            plotOptions: {
                radialBar: {
                    hollow: { size: '55%' },
                    track: { background: '#2a2f3a', strokeWidth: '100%' },
                    dataLabels: {
                        name: { show: false },
                        value: {
                            offsetY: 4,
                            fontSize: '14px',
                            fontWeight: 700,
                            color: '#ffffff',
                            formatter: () => '43%',
                        },
                    },
                },
            },
            stroke: { lineCap: 'round' },
        });
        chart.render();
        this.charts.push(chart);
    }

    private initMiniGauge1(): void {
        const chart = new ApexCharts(document.querySelector('#miniGauge1') as HTMLElement  , {
            chart: { type: 'radialBar', height: 60, width: 60, sparkline: { enabled: true } },
            series: [56],
            colors: ['#f59e0b'],
            plotOptions: {
                radialBar: {
                    hollow: { size: '60%' },
                    track: { background: '#2a2f3a', strokeWidth: '100%' },
                    dataLabels: {
                        name: { show: false },
                        value: {
                            offsetY: 3,
                            fontSize: '10px',
                            fontWeight: 700,
                            color: '#ffffff',
                            formatter: () => '56%',
                        },
                    },
                },
            },
            stroke: { lineCap: 'round' },
        });
        chart.render();
        this.charts.push(chart);
    }

    private initMiniGauge2(): void {
        const chart = new ApexCharts(document.querySelector('#miniGauge2') as HTMLElement, {
            chart: { type: 'radialBar', height: 60, width: 60, sparkline: { enabled: true } },
            series: [42],
            colors: ['#00d9ff'],
            plotOptions: {
                radialBar: {
                    hollow: { size: '60%' },
                    track: { background: '#2a2f3a', strokeWidth: '100%' },
                    dataLabels: {
                        name: { show: false },
                        value: {
                            offsetY: 3,
                            fontSize: '10px',
                            fontWeight: 700,
                            color: '#ffffff',
                            formatter: () => '42%',
                        },
                    },
                },
            },
            stroke: { lineCap: 'round' },
        });
        chart.render();
        this.charts.push(chart);
    }
}