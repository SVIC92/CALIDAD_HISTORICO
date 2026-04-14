import { Button, Stack } from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';
import TableViewIcon from '@mui/icons-material/TableView';
import * as XLSX from 'xlsx';

const normalizeCellValue = (value) => {
  if (value === null || value === undefined) return '';
  if (typeof value === 'object') {
    if (value.nombre) return value.nombre;
    if (value.email) return value.email;
    return JSON.stringify(value);
  }
  return value;
};

const TableExportButtons = ({ columns, data, fileName = 'tabla' }) => {
  const exportRows = data.map((row) => {
    const exportRow = {};

    columns.forEach((column) => {
      exportRow[column.label] = normalizeCellValue(row[column.id]);
    });

    return exportRow;
  });

  const handleExportCsv = () => {
    const worksheet = XLSX.utils.json_to_sheet(exportRows);
    const csv = XLSX.utils.sheet_to_csv(worksheet);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `${fileName}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handleExportExcel = () => {
    const worksheet = XLSX.utils.json_to_sheet(exportRows);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Datos');
    XLSX.writeFile(workbook, `${fileName}.xlsx`);
  };

  return (
    <Stack direction="row" spacing={1} sx={{ mb: 2, justifyContent: 'flex-end' }}>
      <Button
        variant="outlined"
        size="small"
        startIcon={<DownloadIcon />}
        onClick={handleExportCsv}
        disabled={data.length === 0}
      >
        Exportar CSV
      </Button>
      <Button
        variant="contained"
        size="small"
        startIcon={<TableViewIcon />}
        onClick={handleExportExcel}
        disabled={data.length === 0}
      >
        Exportar Excel
      </Button>
    </Stack>
  );
};

export default TableExportButtons;
