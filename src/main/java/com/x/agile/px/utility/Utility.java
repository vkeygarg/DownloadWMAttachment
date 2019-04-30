package com.x.agile.px.utility;

import org.apache.commons.lang.StringUtils;

import com.agile.api.APIException;
import com.agile.api.CommonConstants;
import com.agile.api.IDataObject;
import com.agile.api.IItem;
import com.agile.api.ITable;

public class Utility {
	
	private Utility () {}

	public static ITable getAttTable(IDataObject aglObj, String criteria, String criteriaVal) throws APIException {
		ITable attTable = null;
		Object[] objArr = new Object[] { criteriaVal };
		if (aglObj.getType() == IItem.OBJECT_TYPE) {
			System.out.println("Type is Item: criteria:"+criteria);
			IItem itmObj = (IItem) aglObj;
			System.out.println("rev:"+itmObj.getRevision());
			attTable = StringUtils.isEmpty(criteria) ? itmObj.getAttachments()
					: itmObj.getAttachments().where(criteria, objArr);
			System.out.println("Attachment table size is:"+attTable.size());
		} else {
			System.out.println("In not Item Type");
			if (StringUtils.isEmpty(criteria)) {
				System.out.println("empty criteria");
				attTable = aglObj.getTable(CommonConstants.TABLE_ATTACHMENTS);
			}
			else {
				System.out.println("not empty criteria");
				attTable = aglObj.getTable(CommonConstants.TABLE_ATTACHMENTS).where(criteria, objArr);
			}
		}
		System.out.println("Attachment table size agin is:"+attTable.size());
		return attTable;
	}

}
