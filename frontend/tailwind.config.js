/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        netwatch: {
          dark:  '#0f172a',
          panel: '#1e293b',
          border:'#334155',
          accent:'#38bdf8'
        }
      }
    }
  },
  plugins: []
}
