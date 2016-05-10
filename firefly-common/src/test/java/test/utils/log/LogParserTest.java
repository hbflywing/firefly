package test.utils.log;

import static org.hamcrest.Matchers.*;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.collection.TreeTrie;
import com.firefly.utils.collection.Trie;
import com.firefly.utils.function.Action1;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogConfigParser;
import com.firefly.utils.log.PropertiesLogConfigParser;
import com.firefly.utils.log.XmlLogConfigParser;
import com.firefly.utils.log.file.FileLog;

public class LogParserTest {

	@Test
	public void test() {
		final Trie<Log> xmlLogTree = new TreeTrie<>();
		final Trie<Log> propertiesLogTree = new TreeTrie<>();

		LogConfigParser parser = new XmlLogConfigParser();
		boolean success = parser.parse(new Action1<FileLog>()  {

			@Override
			public void call(FileLog fileLog) {
				xmlLogTree.put(fileLog.getName(), fileLog);
			}
		});
		Assert.assertThat(success, is(true));

		parser = new PropertiesLogConfigParser();
		success = parser.parse(new Action1<FileLog>() {

			@Override
			public void call(FileLog fileLog) {
				propertiesLogTree.put(fileLog.getName(), fileLog);
			}
		});
		Assert.assertThat(success, is(true));

		for (String name : xmlLogTree.keySet()) {
			Log xml = xmlLogTree.get(name);
			Log p = propertiesLogTree.get(name);
			if (p != null) {
				Assert.assertThat(xml, is(p));
			}
		}
	}
}
