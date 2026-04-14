import { Navigate, Outlet, useLocation } from 'react-router-dom';

const RutasProtegidas = ({ allowedRoles }) => {
    const token = localStorage.getItem('token');
    const rol = localStorage.getItem('rol');
    const location = useLocation();

    if (!token) {
        return <Navigate to="/" replace state={{ from: location }} />;
    }

    if (allowedRoles && !allowedRoles.includes(rol)) {
        return <Navigate to="/no-autorizado" replace state={{ from: location.pathname }} />;
    }

    return <Outlet />;
};

export default RutasProtegidas;