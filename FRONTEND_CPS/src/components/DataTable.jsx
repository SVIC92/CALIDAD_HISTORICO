import { 
  Box,
  Table, TableBody, TableCell, TableContainer, 
  TableHead, TableRow, Paper, IconButton, Tooltip 
} from '@mui/material';
import TableExportButtons from './TableExportButtons';
import { useUISettings } from '../context/UISettingsContext';

const renderCellValue = (value) => {
  if (value === null || value === undefined) return '-';
  if (typeof value === 'object') {
    if (value.nombre) return value.nombre;
    if (value.email) return value.email;
    return JSON.stringify(value);
  }
  return value;
};

const DataTable = ({ columns, data, actions, exportFileName = 'tabla' }) => {
  const { preferences } = useUISettings();

  return (
    <Box>
      <TableExportButtons columns={columns} data={data} fileName={exportFileName} />
      <TableContainer component={Paper} sx={{ boxShadow: 3, borderRadius: 2 }}>
        <Table size={preferences.compact ? 'small' : 'medium'} sx={{ minWidth: 650 }}>
          <TableHead sx={{ bgcolor: 'primary.main' }}>
            <TableRow>
              {columns.map((column) => (
                <TableCell 
                  key={column.id} 
                  align={column.align || 'left'}
                  sx={{ color: 'white', fontWeight: 'bold' }}
                >
                  {column.label}
                </TableCell>
              ))}
              {actions && (
                <TableCell align="right" sx={{ color: 'white', fontWeight: 'bold' }}>
                  Acciones
                </TableCell>
              )}
            </TableRow>
          </TableHead>
          <TableBody>
            {data.map((row, index) => (
              <TableRow key={row.id || index} hover>
                {columns.map((column) => (
                  <TableCell key={column.id} align={column.align || 'left'}>
                    {renderCellValue(row[column.id])}
                  </TableCell>
                ))}
                {actions && (
                  <TableCell align="right">
                    {actions.map((action, actionIndex) => (
                      <Tooltip key={actionIndex} title={action.label}>
                        <IconButton 
                          onClick={() => action.onClick(row)} 
                          color={action.color || 'default'}
                        >
                          {action.icon}
                        </IconButton>
                      </Tooltip>
                    ))}
                  </TableCell>
                )}
              </TableRow>
            ))}
            {data.length === 0 && (
              <TableRow>
                <TableCell colSpan={columns.length + (actions ? 1 : 0)} align="center" sx={{ py: 3 }}>
                  No se encontraron registros.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
};

export default DataTable;