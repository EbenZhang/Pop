package com.ezhang.pop.model;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

public class FuelInfoXmlHandler extends DefaultHandler {
	private FuelInfo item;
	private StringBuffer buffer = new StringBuffer();
	public ArrayList<FuelInfo> FuelItems = new ArrayList<FuelInfo>();
    public String PublishDate;

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (localName.equals("item")) {
			item = new FuelInfo();
		}
		super.startElement(uri, localName, qName, attributes);
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		buffer.append(ch, start, length);
		super.characters(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		boolean cleanBuffer = true;
		if (localName.equals("item")) {
			cleanBuffer = false;
			FuelItems.add(item);
		} else if (localName.equals("price")) {
			item.price = Float.parseFloat((buffer.toString().trim()));
		} else if (localName.equals("brand")) {
			item.brand = buffer.toString().trim();
		} else if (localName.equals("address")) {
			item.SetAddressWithoutSuburb(buffer.toString().trim());
		} else if (localName.equals("latitude")) {
			item.latitude = buffer.toString().trim();
		} else if (localName.equals("longitude")) {
			item.longitude = buffer.toString().trim();
		} else if (localName.equals("trading-name")) {
			item.tradingName = buffer.toString().trim();
		}else if (localName.equals("location")) {
            item.SetSuburb(buffer.toString().trim());
        }else if(localName.equals("date")){
            this.PublishDate = buffer.toString().trim();
        }
		if (cleanBuffer)
			buffer.setLength(0);
		super.endElement(uri, localName, qName);
	}
}