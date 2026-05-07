import React from 'react';
import {
  Box, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, IconButton, Tooltip
} from '@mui/material';
import TableExportButtons from './TableExportButtons';
import { useUISettings } from '../context/UISettingsContext';

const renderCellValue = (value) => {
  if (value === null || value === undefined) return '-';
  if (React.isValidElement(value)) return value; // <- FIX para soportar colores o botones sin crashear
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
      <TableContainer
        component={Paper}
        sx={{
          boxShadow: '0 18px 42px rgba(15, 23, 42, 0.10)',
          borderRadius: 2,
          overflow: 'hidden',
        }}
      >
        <Table size={preferences.compact ? 'small' : 'medium'} sx={{ minWidth: 650 }}>
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell
                  key={column.id}
                  align={column.align || 'left'}
                  sx={{
                    color: 'white',
                    fontWeight: 800,
                    background: 'linear-gradient(135deg, #2563eb 0%, #0f766e 100%)',
                    borderBottom: 'none',
                  }}
                >
                  {column.label}
                </TableCell>
              ))}
              {actions && (
                <TableCell align="right" sx={{ color: 'white', fontWeight: 800, background: 'linear-gradient(135deg, #2563eb 0%, #0f766e 100%)', borderBottom: 'none' }}>
                  Acciones
                </TableCell>
              )}
            </TableRow>
          </TableHead>
          <TableBody>
            {data.map((row, index) => (
              <TableRow key={row.id || index} hover sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                {columns.map((column) => (
                  <TableCell key={column.id} align={column.align || 'left'} sx={{ py: preferences.compact ? 1 : 1.5 }}>
                    {/* Soportar función "render" personalizada por columna */}
                    {column.render ? column.render(row) : renderCellValue(row[column.id])}
                  </TableCell>
                ))}
                {actions && (
                  <TableCell align="right" sx={{ py: preferences.compact ? 1 : 1.5 }}>
                    {actions.map((action, actionIndex) => (
                      <Tooltip key={actionIndex} title={action.label}>
                        <IconButton
                          onClick={() => action.onClick(row)}
                          color={action.color || 'default'}
                          sx={{
                            bgcolor: 'action.hover',
                            mr: 0.5,
                            '&:hover': { bgcolor: 'action.selected' },
                          }}
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
                <TableCell colSpan={columns.length + (actions ? 1 : 0)} align="center" sx={{ py: 4, color: 'text.secondary' }}>
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