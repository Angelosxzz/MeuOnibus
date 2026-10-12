package br.com.meuonibus;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.ParcelFileDescriptor;import android.provider.OpenableColumns;import java.io.*;
/** Exposes one read-only update file, only via temporary URI grants. */
public final class ApkProvider extends ContentProvider {
  public static File file(Context context){return new File(new File(context.getCacheDir(),"updates"),"latest.apk");}
  public static Uri uri(Context context){return Uri.parse("content://"+context.getPackageName()+".updates/latest.apk");}
  private File checked(Uri uri)throws FileNotFoundException {if(getContext()==null||!(getContext().getPackageName()+".updates").equals(uri.getAuthority())||!"/latest.apk".equals(uri.getPath())||uri.getQuery()!=null)throw new FileNotFoundException("Arquivo indisponível");File file=file(getContext());if(!file.isFile())throw new FileNotFoundException("Arquivo indisponível");return file;}
  @Override public boolean onCreate(){return true;}
  @Override public String getType(Uri uri){return "application/vnd.android.package-archive";}
  @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException {if(!"r".equals(mode))throw new FileNotFoundException("Somente leitura");return ParcelFileDescriptor.open(checked(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
  @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){try{File file=checked(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(columns);Object[] values=new Object[columns.length];for(int i=0;i<columns.length;i++)values[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?"Meu-Onibus.apk":OpenableColumns.SIZE.equals(columns[i])?file.length():null;cursor.addRow(values);return cursor;}catch(FileNotFoundException e){return null;}}
  @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
  @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
  @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
