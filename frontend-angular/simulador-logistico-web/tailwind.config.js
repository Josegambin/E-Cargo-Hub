/** @type {import('tailwindcss').Config} */
module.exports = {
    darkMode: 'class',
    content: [
        "./src/**/*.{html,ts}",
    ],
    theme: {
        extend: {
            fontFamily: {
                sans: ['Inter', 'system-ui', 'sans-serif'],
                display: ['Poppins', 'Inter', 'sans-serif'],
                mono: ['JetBrains Mono', 'monospace'],
            },
            colors: {
                // Fondo oscuro principal
                bg: {
                    main: '#1a1d24',
                    card: '#232730',
                    hover: '#2a2f3a',
                    border: '#2a2f3a',
                },
                // Acentos
                accent: {
                    cyan:    '#00d9ff',
                    green:   '#22c55e',
                    pink:    '#ec4899',
                    amber:   '#f59e0b',
                    red:     '#ef4444',
                    violet:  '#8b5cf6',
                },
                // Texto
                text: {
                    primary:   '#f1f5f9',
                    secondary: '#94a3b8',
                    muted:     '#64748b',
                },
            },
        },
    },
    plugins: [],
};