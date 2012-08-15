package com.alvazan.orm.layer7.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.orm.api.spi3.meta.DboColumnMeta;
import com.alvazan.orm.api.spi3.meta.DboDatabaseMeta;
import com.alvazan.orm.api.spi3.meta.DboTableMeta;
import com.alvazan.orm.api.spi3.meta.conv.StandardConverters;
import com.alvazan.orm.api.spi9.db.Action;
import com.alvazan.orm.api.spi9.db.Column;
import com.alvazan.orm.api.spi9.db.NoSqlRawSession;
import com.alvazan.orm.api.spi9.db.Persist;
import com.alvazan.orm.api.spi9.db.PersistIndex;
import com.alvazan.orm.api.spi9.db.Remove;
import com.alvazan.orm.api.spi9.db.RemoveIndex;
import com.alvazan.orm.api.spi9.db.Row;
import com.alvazan.orm.api.spi9.db.ScanInfo;

public class NoSqlRawLogger implements NoSqlRawSession {

	private static final Logger log = LoggerFactory.getLogger(NoSqlRawLogger.class);
	@Inject
	@Named("main")
	private NoSqlRawSession session;
	@Inject
	private DboDatabaseMeta databaseInfo;
	
	@Override
	public List<Row> find(String colFamily, List<byte[]> keys) {
		if(log.isInfoEnabled()) {
			logKeys(colFamily, keys);
		}
		return session.find(colFamily, keys);
	}

	private void logKeys(String colFamily, List<byte[]> keys) {
		try {
			logKeysImpl(colFamily, keys);
		} catch(Exception e) {
			log.info("(Exception logging a find operation, turn on trace to see)");
		}
	}
	private void logKeysImpl(String colFamily, List<byte[]> keys) {
		DboTableMeta meta = databaseInfo.getMeta(colFamily);
		if(meta == null)
			return;
		List<String> realKeys = new ArrayList<String>();
		for(byte[] k : keys) {
			Object obj = meta.getIdColumnMeta().convertFromStorage2(k);
			String str = meta.getIdColumnMeta().convertTypeToString(obj);
			realKeys.add(str);
		}
		log.info("CF="+colFamily+" finding keys="+realKeys);
	}

	@Override
	public void sendChanges(List<Action> actions, Object ormFromAbove) {
		if(log.isInfoEnabled()) {
			logInformation(actions);
		}
		session.sendChanges(actions, ormFromAbove);
	}

	private void logInformation(List<Action> actions) {
		try {
			logInformationImpl(actions);
		} catch(Exception e) {
			log.info("(exception logging save actions, turn on trace to see)");
		}
	}
	private void logInformationImpl(List<Action> actions) {
		String msg = "Data being flushed to database in one go=";
		for(Action act : actions) {
			String cf = act.getColFamily();
			if(act instanceof Persist) {
				msg += "\nCF="+cf;
				Persist p = (Persist) act;
				String key = convert(cf, p.getRowKey());
				msg += " persist rowkey="+key;
			} else if(act instanceof Remove) {
				msg += "\nCF="+cf;
				Remove r = (Remove) act;
				String key = convert(cf, r.getRowKey());
				msg += " remove  rowkey="+key;
			} else if(act instanceof PersistIndex) {
				PersistIndex p = (PersistIndex) act;
				msg += "\nCF="+p.getRealColFamily();
				String ind = convert(p);
				msg += " index persist("+ind;
			} else if(act instanceof RemoveIndex) {
				RemoveIndex r = (RemoveIndex) act;
				msg += "\nCF="+r.getRealColFamily();
				String ind = convert(r);
				msg += " index remove ("+ind;
			}
		}
		
		log.info(msg);
	}
	
	private String convert(RemoveIndex r) {
		String msg = "cf="+r.getColFamily()+")=";
		msg += "[rowkey="+StandardConverters.convertFromBytesNoExc(String.class, r.getRowKey())+"]";
		
		try {
			DboTableMeta meta = databaseInfo.getMeta(r.getRealColFamily());
			if(meta == null) 
				return msg+" (meta not found)";
			String colName = r.getColumn().getColumnName();
			DboColumnMeta colMeta = meta.getColumnMeta(colName);
			if(colMeta == null)
				return msg+" (table found, colmeta not found)";
		
			byte[] indexedValue = r.getColumn().getIndexedValue();
			byte[] pk = r.getColumn().getPrimaryKey();
			Object theId = meta.getIdColumnMeta().convertFromStorage2(pk);
			String idStr = meta.getIdColumnMeta().convertTypeToString(theId);
			Object valObj = colMeta.convertFromStorage2(indexedValue);
			String valStr = colMeta.convertTypeToString(valObj);
			
			return msg+"[indexval="+valStr+",to pk="+idStr+"]";
		} catch(Exception e) {
			if(log.isTraceEnabled())
				log.trace("excpetion logging", e);
			return msg + "(exception logging.  turn on trace logs to see)";
		}
	}

	private String convert(String cf, byte[] rowKey) {
		if(rowKey == null)
			return "null";
		
		DboTableMeta meta = databaseInfo.getMeta(cf);
		if(meta == null)
			return "(meta not found)";

		try {
			Object obj = meta.getIdColumnMeta().convertFromStorage2(rowKey);
			return meta.getIdColumnMeta().convertTypeToString(obj);
		} catch(Exception e) {
			if(log.isTraceEnabled())
				log.trace("excpetion logging", e);
			return "(exception converting, turn trace logging on to see it)";
		}
	}

	@Override
	public Iterable<Column> columnRangeScan(ScanInfo info,
			byte[] from, byte[] to, int batchSize) {
		if(log.isInfoEnabled()) {
			logColScan(info, from, to, batchSize);
		}
		return session.columnRangeScan(info, from, to, batchSize);
	}
	
	private void logColScan(ScanInfo info, byte[] from, byte[] to, int batchSize) {
		try {
			String msg = logColScanImpl(info, from, to, batchSize);
			log.info(msg);
		} catch(Exception e) {
			log.info("(Exception trying to log column scan on index cf="+info.getIndexColFamily()+" for cf="+info.getEntityColFamily());
		}
	}

	private String logColScanImpl(ScanInfo info, byte[] from, byte[] to, int batchSize) {
		String msg = "CF="+info.getEntityColFamily()+" index CF="+info.getIndexColFamily();
		if(info.getEntityColFamily() == null)
			return msg + " (meta for main CF can't be looked up)";

		DboTableMeta meta = databaseInfo.getMeta(info.getEntityColFamily());
		if(meta == null)
			return msg + " (meta for main CF was not found)";
		DboColumnMeta colMeta = meta.getColumnMeta(info.getColumnName());
		if(colMeta == null)
			return msg + " (CF meta found but columnMeta not found)";
		
		Object fromObj = colMeta.convertFromStorage2(from);
		String fromStr = colMeta.convertTypeToString(fromObj);
		Object toObj = colMeta.convertFromStorage2(to);
		String toStr = colMeta.convertTypeToString(toObj);
		String rowKey = StandardConverters.convertFromBytesNoExc(String.class, info.getRowKey());
		return msg+" scanning index rowkey="+rowKey+" from="+fromStr+" to="+toStr+" with batchSize="+batchSize;
	}

	@Override
	public void clearDatabase() {
		if(log.isInfoEnabled()) {
			log.info("clearing database");
		}
		session.clearDatabase();
	}

	@Override
	public void start(Map<String, Object> properties) {
		if(log.isInfoEnabled()) {
			log.info("starting NoSQL Service Provider and connecting");
		}
		session.start(properties);
	}

	@Override
	public void close() {
		if(log.isInfoEnabled()) {
			log.info("closing NoSQL Service Provider");
		}
		session.close();
	}

}
