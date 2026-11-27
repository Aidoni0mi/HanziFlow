package com.hanziflow.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.hanziflow.core.*
import java.time.LocalDate

class Store(context: Context): SQLiteOpenHelper(context,"hanziflow.db",null,1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE review(id TEXT PRIMARY KEY, reps INTEGER NOT NULL, lapses INTEGER NOT NULL, interval INTEGER NOT NULL, ease REAL NOT NULL, due TEXT NOT NULL)")
        db.execSQL("CREATE TABLE history(id INTEGER PRIMARY KEY AUTOINCREMENT, word TEXT NOT NULL, grade TEXT NOT NULL, day TEXT NOT NULL)")
        db.execSQL("CREATE TABLE book(id TEXT PRIMARY KEY, name TEXT NOT NULL, kind TEXT NOT NULL, page INTEGER NOT NULL DEFAULT 0, version TEXT NOT NULL DEFAULT 'unknown')")
        db.execSQL("CREATE TABLE quiz(id INTEGER PRIMARY KEY AUTOINCREMENT, correct INTEGER NOT NULL, total INTEGER NOT NULL, day TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Migration $oldVersion -> $newVersion required") }
    fun reviews(): Map<String,Review> {
        val today=LocalDate.now()
        return readableDatabase.rawQuery("SELECT id,reps,lapses,interval,ease,due FROM review",null).use { c -> buildMap {
            while(c.moveToNext()) put(c.getString(0),restoreReview(c.getInt(1),c.getInt(2),c.getLong(3),c.getDouble(4),c.getString(5),today))
        } }
    }
    fun add(id:String) { writableDatabase.execSQL("INSERT OR IGNORE INTO review VALUES(?,0,0,0,2.5,?)",arrayOf(id,LocalDate.now().toString())) }
    fun addAll(ids:List<String>){
        val db=writableDatabase;val today=LocalDate.now().toString();db.beginTransaction()
        try{db.compileStatement("INSERT OR IGNORE INTO review VALUES(?,0,0,0,2.5,?)").use{statement->ids.distinct().forEach{id->statement.clearBindings();statement.bindString(1,id);statement.bindString(2,today);statement.executeInsert()}};db.setTransactionSuccessful()}finally{db.endTransaction()}
    }
    fun grade(id:String, grade:Grade) {
        val db=writableDatabase;val today=LocalDate.now();db.beginTransaction()
        try {
            val old=db.rawQuery("SELECT reps,lapses,interval,ease,due FROM review WHERE id=?",arrayOf(id)).use{c->
                if(c.moveToFirst())restoreReview(c.getInt(0),c.getInt(1),c.getLong(2),c.getDouble(3),c.getString(4),today) else Review(due=today)
            }
            val r=schedule(old,grade,today)
            db.execSQL("INSERT OR REPLACE INTO review VALUES(?,?,?,?,?,?)",arrayOf(id,r.repetitions,r.lapses,r.interval,r.ease,r.due.toString()))
            db.execSQL("INSERT INTO history(word,grade,day) VALUES(?,?,?)",arrayOf(id,grade.name,today.toString()))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun books(): List<Book> = readableDatabase.rawQuery("SELECT id,name,kind,page,version FROM book ORDER BY rowid DESC",null).use { c -> buildList { while(c.moveToNext()) add(Book(c.getString(0),c.getString(1),c.getString(2),c.getInt(3),c.getString(4))) } }
    fun addBook(b: Book) = writableDatabase.execSQL("INSERT INTO book VALUES(?,?,?,?,?)",arrayOf(b.id,b.name,b.kind,b.page,b.version))
    fun page(id:String,page:Int) = writableDatabase.execSQL("UPDATE book SET page=? WHERE id=?",arrayOf(page,id))
    fun version(id:String,version:String) = writableDatabase.execSQL("UPDATE book SET version=? WHERE id=?",arrayOf(version,id))
    fun deleteBook(id:String) = writableDatabase.execSQL("DELETE FROM book WHERE id=?",arrayOf(id))
    fun quiz(correct:Int,total:Int) = writableDatabase.execSQL("INSERT INTO quiz(correct,total,day) VALUES(?,?,?)",arrayOf(correct,total,LocalDate.now().toString()))
    fun lastQuiz(): String = readableDatabase.rawQuery("SELECT correct,total,day FROM quiz ORDER BY id DESC LIMIT 1",null).use { if(it.moveToFirst()) "${it.getInt(0)}/${it.getInt(1)} · ${it.getString(2)}" else "Нет результатов" }
}
data class Book(val id:String,val name:String,val kind:String,val page:Int=0,val version:String="unknown")
fun readWords(context:Context): List<Word> = context.assets.open("words.tsv").bufferedReader().useLines { lines -> lines.drop(1).filter{it.isNotBlank()}.map { line -> val f=line.split('\t'); require(f.size==9); Word(f[0],f[1],f[2],f[3],f[4],f[6],f[7],f[8],f[5],null,"original-unreviewed") }.toList() }
