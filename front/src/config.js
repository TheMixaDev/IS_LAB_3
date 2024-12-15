export default {
	apiUrl: import.meta.env.MODE === 'development' ? 'http://localhost:17274/api' : '/api',
	websocketUrl: import.meta.env.MODE === 'development' ? 'ws://localhost:17274/ws' : '/ws'
};
