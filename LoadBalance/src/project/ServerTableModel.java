package project;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerTableModel extends AbstractTableModel {
    private CopyOnWriteArrayList<ServerManagerInfo> serverList = new CopyOnWriteArrayList<>();
    private final String[] columnNames = {"Port", "Status"};

    public void setServerList(CopyOnWriteArrayList<ServerManagerInfo> serverList) {
        this.serverList = serverList;
        fireTableDataChanged();
    }

    public ServerManagerInfo getServerAt(int rowIndex) {
        return serverList.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return serverList.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ServerManagerInfo serverInfo = serverList.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return serverInfo.getPort();
            case 1:
                return serverInfo.getIsRunning() ? "Running" : "Stopped";
            default:
                return null;
        }
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public void removeServerAt(int rowIndex) {
        serverList.remove(rowIndex);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }
}
