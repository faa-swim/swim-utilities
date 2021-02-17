# SWIM Utilities

Basic set of utilities to help in working with consuming SWIM data

## Installing

  1. Clone this repository
  2. Run mvn clean install
  3. Add dependency to applicable project pom

```xml
	<dependency>
		<groupId>us.dot.faa.swim.utilities</groupId>
		<artifactId>swim-utilities</artifactId>
		<version>1.0</version>
	</dependency>
```

## Current Utilities

### Missed Message Tracker

Provides the ability to track messages consumed via SWIM for missed messages. The utility requires the consuming service to include a sequential id in each message; e.g. AIM FNS CorrelationId. This utility can also be used to track stale message, that is a lack of messages being received from SWIM over a specified time frame; e.g. 10 minutes.

```java

	int missedMessageTrackerScheduleRate = 10; //seconds
	int missedMessageTriggerTime = 5; //minutes
	int staleMessageTriggerTime = 10; // minutes

	MissedMessageTracker missedMessageTracker = new MissedMessageTracker(missedMessageTrackerScheduleRate,
				missedMessageTriggerTime, staleMessageTriggerTime) {
		
		@Override
		public void onMissed(Map<Long, Instant> missedMessages) {
			//logic to handle missed messages
		}

		@Override
		public void onStale(Long lastRecievedId, Instant lastRecievedTime) {
			//logic to handle stale messages
		}
	};
		
	missedMessageTracker.start();

```

### Xml Splitter Sax Parser
Provides the ability to split xml files according to depth.


```java	

	//reads xml file doc.xml and slits by a depth of 2

	FileInputStream filInputStream = new FileInputStream("doc.xml");

	XmlSplitterSaxParser parser = new XmlSplitterSaxParser(xml -> {
        //logic here to handle parsed element (xml)
    }, 2);

    final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);
    saxParserFactory.setFeature("http://xml.org/sax/features/namespaces", true);
    saxParserFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
    saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

    SAXParser saxParser = saxParserFactory.newSAXParser();
    XMLReader xmlReader = saxParser.getXMLReader();
    xmlReader.setContentHandler(parser);
    SaxParserErrorHandler parsingErrorHandeler = new SaxParserErrorHandler();
    xmlReader.setErrorHandler(parsingErrorHandeler);
    xmlReader.parse(new InputSource(filInputStream));
    if (!parsingErrorHandeler.isValid()) {
        throw new Exception("Failed to Parse");
    }

```