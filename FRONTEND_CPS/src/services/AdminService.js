import axios from '../API/axios';

const AdminService = {
    dashboard: async () => {
        const respuesta = await axios.get('/admin/dashboard');
        return respuesta.data;
    },
};

export default AdminService;
