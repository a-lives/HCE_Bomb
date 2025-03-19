package com.example.nfcbomb;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.Arrays;


public class HASBomb extends HostApduService {

    public static final ComponentName COMPONENT = new ComponentName("com.example.nfcbomb", HASBomb.class.getName());

    private static final byte[] SELECT_APPLICATION = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xA4, // INS	- Instruction - Instruction code
            (byte) 0x04, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x00, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x07, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x85, (byte) 0x01,
            (byte) 0x01, // NDEF Tag Application name D2 76 00 00 85 01 01
            (byte) 0x00  // Le field	- Maximum number of bytes expected in the data field of
            // the response to the command
    };

    private static final byte[] SELECT_CAPABILITY_CONTAINER = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // INS	- Instruction - Instruction code
            (byte) 0x00, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x0c, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xe1, (byte) 0x03 // file identifier of the CC file
    };

    private static final byte[] SELECT_NDEF_FILE = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // Instruction byte (INS) for Select command
            (byte) 0x00, // Parameter byte (P1), select by identifier
            (byte) 0x0c, // Parameter byte (P1), select by identifier
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xE1, (byte) 0x04 // file identifier of the NDEF file retrieved from the CC file
    };

    private final static byte[] CAPABILITY_CONTAINER_FILE = new byte[]{
            0x00, 0x0f, // CCLEN
            0x20, // Mapping Version
            0x00, 0x3b, // Maximum R-APDU data size
            0x00, 0x34, // Maximum C-APDU data size
            0x04, 0x06, // Tag & Length
            (byte) 0xe1, 0x04, // NDEF File Identifier
            (byte) 0x00, (byte) 0xff, // Maximum NDEF size, do NOT extend this value
            0x00, // NDEF file read access granted
            (byte) 0xff, // NDEF File write access denied
    };

    // Status Word success
    private final static byte[] SUCCESS_SW = new byte[]{
            (byte) 0x90,
            (byte) 0x00,
    };
    // Status Word failure
    private final static byte[] FAILURE_SW = new byte[]{
            (byte) 0x6a,
            (byte) 0x82,
    };

    private byte[] mNdefRecordFile;
    private boolean mAppSelected; // true when SELECT_APPLICATION detected
    private boolean mCcSelected; // true when SELECT_CAPABILITY_CONTAINER detected
    private boolean mNdefSelected; // true when SELECT_NDEF_FILE detected

    @Override
    public void onCreate() {
        super.onCreate();

        // 状態のクリア
        mAppSelected = false;
        mCcSelected = false;
        mNdefSelected = false;

        Log.i("HCE","Service on create");
        // the maximum length is 246 so do not extend this value
        byte[] ndefDefaultMessage = getNdefMessage();
        int nlen = ndefDefaultMessage.length;
        mNdefRecordFile = new byte[nlen + 2];
        mNdefRecordFile[0] = (byte)((nlen & 0xff00) / 256);
        mNdefRecordFile[1] = (byte)(nlen & 0xff);
        System.arraycopy(ndefDefaultMessage, 0, mNdefRecordFile, 2, nlen);
    }

    private byte[] getNdefMessage() {
//        NdefRecord appRecord = NdefRecord.createApplicationRecord("com.hypergryph.arknights");
//        NdefRecord appRecord2 = NdefRecord.createApplicationRecord("com.hypergryph.arknights.bilibili");
//        NdefRecord uriRecord = NdefRecord.createUri("https://www.hypergryph.com/");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String appstr = sharedPreferences.getString("Ndef_package", "com.hypergryph.arknights");
        String uristr = sharedPreferences.getString("Ndef_uri","https://www.hypergryph.com/");
        boolean urifirst = sharedPreferences.getBoolean("Uri_first",false);
        NdefRecord appRecord = NdefRecord.createApplicationRecord(appstr);
        NdefRecord uriRecord = NdefRecord.createUri(uristr);
        if(urifirst){
            NdefMessage message = new NdefMessage(new NdefRecord[]{appRecord,uriRecord});
            return message.toByteArray();
        }else {
            NdefMessage message = new NdefMessage(new NdefRecord[]{uriRecord, appRecord});
            return message.toByteArray();
        }
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        // 1. 检查是否为 SELECT AID 指令
        Log.i("HCE","processCommandApdu "  + Arrays.toString(commandApdu));
        if (Arrays.equals(SELECT_APPLICATION, commandApdu)) {
            mAppSelected = true;
            mCcSelected = false;
            mNdefSelected = false;
            return SUCCESS_SW;
            // check if commandApdu qualifies for SELECT_CAPABILITY_CONTAINER
        } else if (mAppSelected && Arrays.equals(SELECT_CAPABILITY_CONTAINER, commandApdu)) {
            mCcSelected = true;
            mNdefSelected = false;
            return SUCCESS_SW;
            // check if commandApdu qualifies for SELECT_NDEF_FILE
        } else if (mAppSelected && Arrays.equals(SELECT_NDEF_FILE, commandApdu)) {
            // NDEF
            mCcSelected = false;
            mNdefSelected = true;
            return SUCCESS_SW;
            // check if commandApdu qualifies for // READ_BINARY
        } else if (commandApdu[0] == (byte) 0x00 && commandApdu[1] == (byte) 0xb0) {
            // READ_BINARY
            // get the offset an le (length) data
            //System.out.println("** " + Utils.bytesToHex(commandApdu) + " in else if
            // (commandApdu[0] == (byte)0x00 && commandApdu[1] == (byte)0xb0) {");
            int offset = (0x00ff & commandApdu[2]) * 256 + (0x00ff & commandApdu[3]);
            int le = 0x00ff & commandApdu[4];

            byte[] responseApdu = new byte[le + SUCCESS_SW.length];

            if (mCcSelected && offset == 0 && le == CAPABILITY_CONTAINER_FILE.length) {
                System.arraycopy(CAPABILITY_CONTAINER_FILE, offset, responseApdu, 0, le);
                System.arraycopy(SUCCESS_SW, 0, responseApdu, le, SUCCESS_SW.length);
                return responseApdu;
            } else if (mNdefSelected) {
                if (offset + le <= mNdefRecordFile.length) {
                    System.arraycopy(mNdefRecordFile, offset, responseApdu, 0, le);
                    System.arraycopy(SUCCESS_SW, 0, responseApdu, le, SUCCESS_SW.length);
                    return responseApdu;
                }
            }
        }
        return FAILURE_SW; // 错误状态码
    }

    @Override
    public void onDeactivated(int reason) {
        // 当 NFC 连接断开时触发
        Log.i("HCE","onDeactivated");
        mAppSelected = false;
        mCcSelected = false;
        mNdefSelected = false;
    }
}
