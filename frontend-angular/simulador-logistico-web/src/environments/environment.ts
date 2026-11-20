export const environment = {
    production: false,
    apiUrl: 'http://localhost:8081',
    identityUrl: 'http://localhost:8180',
    identityRealm: 'e-cargo-hub',
    identityClientId: 'ecargo-dashboard',
    wsUrl: 'http://localhost:8081/ws',              // SockJS (no usado)
    wsUrlNative: 'ws://localhost:8081/ws-native'    // 👈 WebSocket nativo
};