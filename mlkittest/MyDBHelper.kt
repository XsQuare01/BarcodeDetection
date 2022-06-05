package com.example.mlkittest

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MyDBHelper(val context: Context): SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object{
        const val DB_NAME = "fridgedb.db"
        const val DB_VERSION = 1
        const val TABLE_NAME = "products"
        const val PID = "pid"
        const val FID = "fid"
        const val PNAME = "pname"
        const val PQUANTITY = "pquantity"
        const val EXPDATE = "expdate"

        const val TEMP_TABLE_NAME = "fridges"
        const val TEMP_FID = "tid"
        const val TEMP_FNAME = "tname"
    }

    // Table 생성
    override fun onCreate(p0: SQLiteDatabase?) {
        val createTable = "create table if not exists $TABLE_NAME(" +
                "$PID integer primary key autoincrement, " +
                "$FID integer, " +
                "$PNAME text, " +
                "$PQUANTITY integer, " +
                "$EXPDATE integer);"
        p0!!.execSQL(createTable)


        val createTempTable = "create table if not exists $TEMP_TABLE_NAME(" +
                "$TEMP_FID integer primary key autoincrement, " +
                "$TEMP_FNAME text);"

        // 시험용으로 만든 fridge db
        p0.execSQL(createTempTable)

    }

    // db version 변경 시, 근데이거 무슨 뜻일까
    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        val dropTable = "drop table if exists $TABLE_NAME"

        p0!!.execSQL(dropTable)
        onCreate(p0)
    }

    // product 객체를 생성해서 넘겨주면 db에 data 삽입
    fun insertProduct(product: Product): Boolean {
        val value = ContentValues()
        value.put(FID, product.fid)
        value.put(PNAME, product.pname)
        value.put(PQUANTITY, product.pquantity)
        value.put(EXPDATE, product.expdate)

        val db = writableDatabase
        val flag = db.insert(TABLE_NAME, null, value) > 0

        db.close()
        return flag

    }

    // 임시 data 삽입
    fun insertTempProduct(fridge: Fridge): Boolean{
        val value = ContentValues()
        value.put(TEMP_FNAME, fridge.fname)

        val db = writableDatabase
        val flag = db.insert(TEMP_TABLE_NAME, null, value) > 0

        db.close()
        return flag

    }

    // 이거 맞나...?? tempDB 에서 name 만 뽑아오는 함수
    fun getTNameRecords(){
        val query = "select * from $TEMP_TABLE_NAME"
        val db = readableDatabase

        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()

        val activity = context as MainActivity3
        if(cursor.isAfterLast){
            return
        }
        else{
            // 지우고 다시 쓰기
            activity.items.clear()

            do{
                // 아 몰라 되겠지
                val tmp = Fridge(cursor.getInt(0), cursor.getString(1))
                activity.items.add(tmp)
            }while (cursor.moveToNext())

        }

    }
}