/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.rs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsic3DLUT;
import android.support.v8.renderscript.Type;
import android.view.View;
import android.widget.ImageView;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView mImageView;
    RenderScript mRs;
    Bitmap mBitmap;
    Bitmap mLutBitmap;
    ScriptIntrinsic3DLUT mScriptlut;
    Bitmap mOutputBitmap;
    Allocation mAllocIn;
    Allocation mAllocOut;
    Allocation mAllocCube;
    int mFilter = 0;
    int[] mLut3D = {
            R.drawable.lut_ad1920,
            R.drawable.lut_ancient,
            R.drawable.lut_bleachedblue,
            R.drawable.lut_blues,
            R.drawable.lut_bw,
            R.drawable.lut_celsius,
            R.drawable.lut_chest,
            R.drawable.lut_breeze
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setOnClickListener(this);
        mRs = RenderScript.create(this);
        Background background = new Background();
        background.execute();
    }

    @Override
    public void onClick(View v) {
        mFilter = (1 + mFilter) % (mLut3D.length + 1);
        Background background = new Background();
        background.execute();
    }

    class Background extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            int redDim, greenDim, blueDim;
            int w, h;
            int[] lut;

            if (mScriptlut == null) {
                mScriptlut = ScriptIntrinsic3DLUT.create(mRs, Element.U8_4(mRs));
            }
            if (mBitmap == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(),
                        R.drawable.bugs);

                mOutputBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), mBitmap.getConfig());

                mAllocIn = Allocation.createFromBitmap(mRs, mBitmap);
                mAllocOut = Allocation.createFromBitmap(mRs, mOutputBitmap);
            }
            if (mFilter != 0) {
                mLutBitmap = BitmapFactory.decodeResource(getResources(), mLut3D[mFilter - 1]);
                w = mLutBitmap.getWidth();
                h = mLutBitmap.getHeight();
//                redDim = w / h;
//                greenDim = redDim;
//                blueDim = redDim;

                redDim = greenDim = blueDim = 64;
                int[] pixels = new int[w * h];
                lut = new int[w * h];
                mLutBitmap.getPixels(pixels, 0, w, 0, 0, w, h);

                int i = 0;
                //512*512的查色表，根据b的大小，划分 8*8的小块
                for (int r=0; r<redDim; r++){
                    for (int g=0; g<greenDim; g++){
                        for (int b=0; b<blueDim; b++){

                            int blockX = b%8;
                            int blockY = b/8;

                            lut[i++] = pixels[(blockY * 64 + g) * 512 + (blockX * 64 + r)];
                        }
                    }
                }

            } else {
                // identity filter provided for refrence
                redDim = greenDim = blueDim = 64;
                int i = 0;
//                512*512 == 64 * 64 * 64
                lut = new int[redDim * greenDim * blueDim];

                for (int r = 0; r < redDim; r++) {
                    for (int g = 0; g < greenDim; g++) {
                        for (int b = 0; b < blueDim; b++) {
                            int bcol = (b * 255) / blueDim;
                            int gcol = (g * 255) / greenDim;
                            int rcol = (r * 255) / redDim;
                            lut[i++] = bcol | (gcol << 8) | (rcol << 16);
                        }
                    }
                }
            }
            Type.Builder tb = new Type.Builder(mRs, Element.U8_4(mRs));
            tb.setX(redDim).setY(greenDim).setZ(blueDim);
            Type t = tb.create();
            mAllocCube = Allocation.createTyped(mRs, t);
            mAllocCube.copyFromUnchecked(lut);

            mScriptlut.setLUT(mAllocCube);
            mScriptlut.forEach(mAllocIn, mAllocOut);

            mAllocOut.copyTo(mOutputBitmap);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mImageView.setImageBitmap(mOutputBitmap);
        }
    }
}
