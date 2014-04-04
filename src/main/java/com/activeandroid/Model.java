package com.activeandroid;

/*
 * Copyright (C) 2010 Michael Pardo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;

import java.util.List;

@SuppressWarnings("unchecked")
public abstract class Model implements IModelInfo{
	private TableInfo mTableInfo;

	public Model() {
		mTableInfo = Cache.getTableInfo(getClass());
	}

    private long mId;

    /**
     * Use This method to return the values of your primary key, must be separated by comma delimiter in order of declaration
     * Also each object thats instance of {@link java.lang.Number} must be DataBaseUtils.sqlEscapeString(object.toString)
     * @return
     */
    @Override
	public abstract String getId();

    @Override
	public final void delete() {
        SQLiteUtils.delete(this);
	}

    @Override
	public final void save() {
        SQLiteUtils.save(Cache.getClassSerializerForType(Model.class), this);
	}

    @Override
    public boolean exists(){
        return SQLiteUtils.exists((Class<Model>) getClass(), this);
    }

   /* public void update(){

    }*/

    protected final <T extends Model> List<T> getManyFromField(Class<T> type,Object field, String foreignKey){
        return new Select().from(type).where(Cache.getTableName(type) + "." + foreignKey + "=?", field).execute();
    }

    protected final <T extends Model> List<T> getManyFromFieldWithSort(Class<T> type,Object field, String foreignKey, String sort){
        return new Select().from(type).orderBy(sort).where(Cache.getTableName(type) + "." + foreignKey + "=?", field).execute();
    }

	@Override
	public String toString() {
		return mTableInfo!=null? mTableInfo.getTableName() + "@" + getId() : "No Table for: " + getClass() + "@" + getId();
	}

    @Override
    public long getRowId(){
        return mId;
    }

    public TableInfo getTableInfo() {
        return mTableInfo;
    }

    @Override
    public void setRowId(long id) {
        mId = id;
    }

    @Override
    public String getTableName() {
        return mTableInfo.getTableName();
    }

    @Override
    public Class<?> getModelClass() {
        return getClass();
    }
}
