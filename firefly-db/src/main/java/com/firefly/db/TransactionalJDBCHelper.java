package com.firefly.db;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.ResultSetHandler;

import com.firefly.utils.Assert;
import com.firefly.utils.function.Func1;
import com.firefly.utils.function.Func2;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class TransactionalJDBCHelper {

	private final static Log log = LogFactory.getInstance().getLog("firefly-system");

	private static final ThreadLocal<Transaction> transaction = new ThreadLocal<>();
	private final JDBCHelper jdbcHelper;

	public TransactionalJDBCHelper(DataSource dataSource) {
		this(new JDBCHelper(dataSource,
				JDBCHelper.getQueryRunner(dataSource, log.isDebugEnabled() || log.isTraceEnabled())));
	}

	public TransactionalJDBCHelper(JDBCHelper jdbcHelper) {
		this.jdbcHelper = jdbcHelper;
	}

	public JDBCHelper getJdbcHelper() {
		return jdbcHelper;
	}

	public <T> T queryForSingleColumn(final String sql, final Object... params) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, T>() {

			@Override
			public T call(Connection connection, JDBCHelper helper) {
				return helper.queryForSingleColumn(connection, sql, params);
			}
			
		});
	}

	public <T> T queryForObject(final String sql, final Class<T> t, final Object... params) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, T>() {

			@Override
			public T call(Connection connection, JDBCHelper helper) {
				return helper.queryForObject(connection, sql, t, params);
			}
			
		});
	}

	public <T> T queryForObject(final String sql, final Class<T> t, final BeanProcessor beanProcessor, final Object... params) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, T>() {

			@Override
			public T call(Connection connection, JDBCHelper helper) {
				return helper.queryForObject(connection, sql, t, beanProcessor, params);
			}
			
		});
	}

	public <T> T queryById(final Class<T> t, final Object id) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, T>() {

			@Override
			public T call(Connection connection, JDBCHelper helper) {
				return helper.queryById(connection, t, id);
			}
			
		});
	}

	public <K, V> Map<K, V> queryForBeanMap(final String sql, final Class<V> t, final Object... params) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, Map<K, V>>() {

			@Override
			public Map<K, V> call(Connection connection, JDBCHelper helper) {
				return helper.queryForBeanMap(connection, sql, t, params);
			}
			
		});
	}

	public <K, V> Map<K, V> queryForBeanMap(final String sql, final Class<V> t, final BeanProcessor beanProcessor, final Object... params) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, Map<K, V>>() {

			@Override
			public Map<K, V> call(Connection connection, JDBCHelper helper) {
				String columnName = helper.getDefaultBeanProcessor().getIdColumnName(t);
				Assert.notNull(columnName);

				return helper.queryForBeanMap(connection, sql, t, columnName, beanProcessor, params);
			}
			
		});
	}

	public <T> List<T> queryForList(final String sql, final Class<T> t, final Object... params) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, List<T>>() {

			@Override
			public List<T> call(Connection connection, JDBCHelper helper) {
				return helper.queryForList(connection, sql, t, params);
			}
			
		});
	}

	public <T> List<T> queryForList(final String sql, final Class<T> t, final BeanProcessor beanProcessor, final Object... params) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, List<T>>() {

			@Override
			public List<T> call(Connection connection, JDBCHelper helper) {
				return helper.queryForList(connection, sql, t, beanProcessor, params);
			}
			
		});
	}

	public int update(final String sql, final Object... params) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, Integer>() {

			@Override
			public Integer call(Connection connection, JDBCHelper helper) {
				return helper.update(connection, sql, params);
			}
			
		});
	}

	public int updateObject(final Object object) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, Integer>() {

			@Override
			public Integer call(Connection connection, JDBCHelper helper) {
				return helper.updateObject(connection, object);
			}
			
		});
	}

	public <T> T insert(final String sql, final Object... params) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, T>() {

			@Override
			public T call(Connection connection, JDBCHelper helper) {
				return helper.insert(connection, sql, params);
			}
			
		});
	}

	public <T> T insertObject(final Object object) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, T>() {

			@Override
			public T call(Connection connection, JDBCHelper helper) {
				return helper.insertObject(connection, object);
			}
			
		});
	}

	public int deleteById(final Class<?> t, final Object id) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, Integer>() {

			@Override
			public Integer call(Connection connection, JDBCHelper helper) {
				return helper.deleteById(connection, t, id);
			}
			
		});
	}

	public int[] batch(final String sql, final Object[][] params) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, int[]>() {

			@Override
			public int[] call(Connection connection, JDBCHelper helper) {
				int[] ret = null;
				try {
					ret = helper.getRunner().batch(connection, sql, params);
				} catch (Exception e) {
					rollback();
					log.error("batch exception", e);
				}
				return ret;
			}
			
		});
	}

	public <T> T insertBatch(final String sql, final ResultSetHandler<T> rsh, final Object[][] params) {
		return _executeTransaction(new Func2<Connection, JDBCHelper, T>() {

			@Override
			public T call(Connection connection, JDBCHelper helper) {
				T ret = null;
				try {
					ret = helper.getRunner().insertBatch(connection, sql, rsh, params);
				} catch (Exception e) {
					rollback();
					log.error("insert batch exception", e);
				}
				return ret;
			}
			
		});
	}

	public <T> T executeTransaction(Func1<TransactionalJDBCHelper, T> func) {
		beginTransaction();
		try {
			T ret = func.call(this);
			commit();
			return ret;
		} catch (Throwable t) {
			rollback();
			log.error("the transaction exception", t);
		} finally {
			endTransaction();
		}
		return null;
	}

	private <T> T _executeTransaction(Func2<Connection, JDBCHelper, T> func) {
		beginTransaction();
		try {
			T ret = func.call(getConnection(), jdbcHelper);
			commit();
			return ret;
		} catch (Throwable t) {
			rollback();
			log.error("the transaction exception", t);
		} finally {
			endTransaction();
		}
		return null;
	}

	private void beginTransaction() {
		getTransaction().beginTransaction();
	}

	public Connection getConnection() {
		return getTransaction().getConnection();
	}

	public void commit() {
		getTransaction().commit();
	}

	public void rollback() {
		getTransaction().rollback();
	}

	private void endTransaction() {
		getTransaction().endTransaction();
	}

	private Transaction getTransaction() {
		Transaction t = transaction.get();
		if (t == null) {
			t = new Transaction();
			transaction.set(t);
		}
		return t;
	}

	enum Status {
		INIT, START, COMMIT, ROLLBACK, END
	}

	class Transaction {
		private Connection connection;
		private Status status = Status.INIT;
		private int count = 0;

		synchronized void beginTransaction() {
			if (status == Status.INIT) {
				connection = jdbcHelper.getConnection();
				jdbcHelper.setAutoCommit(connection, false);
				status = Status.START;
			}
			count++;
			log.debug("begin transaction {}", count);
		}

		synchronized Connection getConnection() {
			check();
			return connection;
		}

		synchronized void rollback() {
			check();
			status = Status.ROLLBACK;
		}

		synchronized void commit() {
			check();
			if (status != Status.ROLLBACK) {
				status = Status.COMMIT;
			}
		}

		private synchronized void check() {
			if (status == Status.INIT) {
				throw new IllegalStateException("The transaction has not started, " + status);
			}
			if (status == Status.END) {
				throw new IllegalStateException("The transaction has ended, " + status);
			}
		}

		synchronized void endTransaction() {
			count--;
			if (count == 0) {
				switch (status) {
				case START:
				case COMMIT:
					jdbcHelper.commit(connection);
					break;
				case ROLLBACK:
					jdbcHelper.rollback(connection);
					break;
				default:
					break;
				}

				jdbcHelper.close(connection);
				transaction.set(null);
				status = Status.END;
			}
			log.debug("end transaction {}", count);
		}

	}

}
