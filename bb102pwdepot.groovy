import java.text.SimpleDateFormat 
import java.util.Date

def cli = new CliBuilder(usage: 'bb102pwdepot.groovy [bb10-password.csv] [password-depot-outfile.xml]')
// Create the list of options.
cli.with {
	h longOpt: 'help', 'Show usage information'
}
def options = cli.parse(args)
if (!options) {
	cli.usage()
	return
}
def extraArguments = options.arguments()
if (!extraArguments || extraArguments.size() < 2) {
	cli.usage()
	return
}
// Show usage text when -h or --help option is used.
if (options.h) {
	cli.usage()
	return
}

inputFilename = extraArguments[0]
outputFilename = extraArguments[1]

lines = readCsv(inputFilename)

writePasswordDepotFile(lines, outputFilename) 

/////////////////////////////////
// Read BB10 Passwords CSV format

def decodeCsvValue(val) {
	
	if(val.size() < 2 ) {
		return val
	}

	if(val.startsWith('"') && val.endsWith('"')) {
		// Quoted value
		// remove first and last quote
		val = val.substring(1, val.size() - 1)
	}

	// replace escaped quotes by single quote
	val = val.replaceAll('""', '"')
	return val
}

def readCsv(filename) {

	// remember as list of strings
	headerLine = null

	fileContents = new File(filename).getText('UTF-8')

	// current char index
	pos = 0
	// begin index of current value
	tokenStart = 0

	length = fileContents.size()

	// used to collect list of string values for the current line
	currentLine = []

	// collect result as list of dict(column header -> value)
	lines = []

	while(pos < length) {
		curChar = fileContents[pos]
		switch(curChar) {
			case ',':
				currentLine << decodeCsvValue(fileContents.substring(tokenStart, pos))
				pos++
				tokenStart = pos
				break
			case '\n':
				currentLine << decodeCsvValue(fileContents.substring(tokenStart, pos))
				if(headerLine == null) {
					// first time we get here, remember header line
					headerLine = currentLine
				} else {
					// add lines as dictionary
					if(currentLine.size() > 0) {
						dict = [:]
						for(i=0; i<headerLine.size() && i<currentLine.size(); i++) {
							dict[headerLine[i]] = currentLine[i]
						}
						lines << dict
					}
				}
				// start new line
				currentLine = []
				pos++
				tokenStart = pos
				break
			case '"':
				if(tokenStart == pos) {
					// Quoted value
					scanningQuotedValueFinished = false
					pos++
					while(!scanningQuotedValueFinished && pos < length) {
						curChar = fileContents[pos]
						switch(curChar) {
							case '"':
								// Quote
								// check next char
								peekChar = ''
								if(pos + 1 < length) {
									peekChar = fileContents[pos + 1]
								}
								switch(peekChar) {
									case '"':
										// "" means an escaped quote
										// increment pos to second quote
										pos++
										break
									default:
										// we are sure that we saw the closing quote
										scanningQuotedValueFinished = true
										break
								}
								break
							default:
								break
						}
						// go on scanning for the end of quoted value
						pos++
					}
					break
				}
				// pass through
			default: 
				pos++
				break
		}
	}
	// add final line
	if(currentLine.size() > 0) {
		dict = [:]
		for(i=0; i<headerLine.size() && i<currentLine.size(); i++) {
			dict[headerLine[i]] = currentLine[i]
		}
		lines << dict
	}

	return lines
}


//////////////////////////////////
// Write Password depot XML format

def perepareXmlValue(val) {
	val = groovy.xml.XmlUtil.escapeXml(val)
	// Password depot expects windows style line endings
	val = val.replaceAll('\n', '\r\n')
	return val
}

def dateFromTimestamp(timestampString) {
	if(timestampString == null) {
		return null;
	}
	timestampString = timestampString.trim()
	if(timestampString == '') {
		return null;
	}
	long secondsSince1970 = Long.parseLong(timestampString)
	Date date = new Date(secondsSince1970 * 1000)
	return date
}

def writePasswordDepotFile(lines, filename) {

	File outfile = new File(filename)

	xmlHeader = """<?xml version="1.0" encoding="utf-8"?>
		<PASSWORDFILE xmlns="https://www.password-depot.de/schemas/passwordfile/7.0/passwordfile.xsd">
		<HEADER>
		<APPLICATION>Password Depot</APPLICATION>
		<VERSION>10.0.0</VERSION>
		</HEADER>
		<PASSWORDS>
		<GROUP NAME="pw-exp">"""
	
	xmlFooter = """
		</GROUP>
		</PASSWORDS>
		</PASSWORDFILE>
		"""

	dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
	
	outfile.withWriter('UTF-8') { writer ->

		writer.write(xmlHeader)
	
		lines.each { line -> 

			name_escaped = perepareXmlValue(line['name'])
			password_escaped = perepareXmlValue(line['password'])
			username_escaped = perepareXmlValue(line['username'])
			url_escaped = perepareXmlValue(line['url'])
			extra_escaped = perepareXmlValue(line['extra'])
			uid_escaped = perepareXmlValue(line['uid'])

			lastModifiedTime = dateFromTimestamp(line['lastModifiedTime'])
			lastModifiedTime_formatted = ''
			if(lastModifiedTime != null) {
				lastModifiedTime_formatted = dateFormat.format(lastModifiedTime)
			}

			item = """
				<ITEM>
				<DESCRIPTION>$name_escaped</DESCRIPTION>
				<PASSWORD>$password_escaped</PASSWORD>
				<USERNAME>$username_escaped</USERNAME>
				<URL>$url_escaped</URL>
				<COMMENT>$extra_escaped</COMMENT>
				<SOURCEID>$uid_escaped</SOURCEID>
				<LASTMODIFIED FMT="dd.mm.yyyy hh:mm:ss">$lastModifiedTime_formatted</LASTMODIFIED>
				</ITEM>"""

			writer.write(item)
		}

		writer.write(xmlFooter)
	}
}
