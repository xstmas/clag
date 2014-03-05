// regularly (not every tick) measure cpu time consumed by chunks. if a chunk is causing lag, slow down time inside that chunk : skip ticks

package clag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ReportedException;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.ForgeDummyContainer;
import cpw.mods.fml.common.FMLLog;

// based on https://github.com/nallar/TickProfiler/blob/master/src/common/me/nallar/tickprofiler/minecraft/profiling/EntityTickProfiler.java
public class CLagTileEntityTicker {
    public static CLagTileEntityTicker instance = new CLagTileEntityTicker();
    boolean bIsProfiling = false;
    boolean bIsWarning = false;
    public static int profile_interval = 10*20; // ticks, 10*20 = 10 seconds
    public static int warn_interval = 10*60*20; // ticks, 5*60*20 = 5 minutes
    public static int last_warn_tick = Integer.MIN_VALUE;
    public int cur_ticknum = 0;

    public static final long TIMESUM_MICRO = 1000*1; // System.nanoTime -> nano*1000 = micro, micro*1000 = milli
    public static final long TIMESUM_MILLI = 1000*TIMESUM_MICRO;
    public static final long TIMESUM_SECOND = 1000*TIMESUM_MILLI;
    public static final long TIMESUM_TICK = TIMESUM_SECOND/20;
    public static long timesum_min_slowA = TIMESUM_TICK/100;
    public static long timesum_min_slowB = TIMESUM_TICK/50;
    public static long timesum_min_slowC = TIMESUM_TICK/20;
    public static long timesum_warn = 0;
    // public long timesum_min_slow = 10;
    public static int warn_radius = 16*8; // in tiles/blocks, chunk=16
    public static String warn_text = "Warning! a chunk near you is causing lag, so time was slowed there.";

    public static int slow_down_factorA = 4;
    public static int slow_down_factorB = 10;
    public static int slow_down_factorC = 20;
    

    public HashSet<ChunkInfo> warnset = new HashSet<ChunkInfo>(); //HashSet instance without any element

    // ----------------------------- constructor
    
    public CLagTileEntityTicker ()
    {

    }

    // ----------------------------- utils
    
    public int getSlowFactor (ChunkInfo o)
    {
    	if (cur_ticknum < o.force_slow_until_tick) return slow_down_factorC;
        if (o.iTimeSum >= timesum_min_slowC) return slow_down_factorC;
        if (o.iTimeSum >= timesum_min_slowB) return slow_down_factorB;
        if (o.iTimeSum >= timesum_min_slowA) return slow_down_factorA;
        return 1;
    }
    
    // ----------------------------- warn
    
    public void warnStartTick ()
    {
    	// warn time
    	timesum_warn = timesum_min_slowA;
    	if (timesum_warn > timesum_min_slowB) timesum_warn = timesum_min_slowB;
    	if (timesum_warn > timesum_min_slowC) timesum_warn = timesum_min_slowC;
    	warnset.clear();
    }
    
    public void warnEndTick ()
    {
		for (ChunkInfo o : warnset) {
			warnNearestPlayer(o);
		}
    }
    
    public void warnNearestPlayer (ChunkInfo o)
    {
    	EntityPlayerMP p = CLagUtils.findNearestPlayer(o.pos.dim,o.pos.cx,o.pos.cz,warn_radius);
    	if (p == null) return;
    	String txt = warn_text;
    	txt += " "+(o.pos.cx*16+8)+","+(o.pos.cz*16+8);
    	txt += " : "+o.worst_x+","+o.worst_y+","+o.worst_z;
    	CLagUtils.sendChat(p,txt);
    }
    
    // ----------------------------- tick

    public void StartTick (int iTickNum) {
        cur_ticknum = iTickNum;
        bIsProfiling = (iTickNum % profile_interval) == 0;
        bIsWarning = false;
        if (bIsProfiling) 
        {
        	worst_chunk_time = 0;
        	CLagUtils.debug("CLagTileEntityTicker:StartTick profile "+iTickNum);
        	
        	// warning nearby players
        	if (iTickNum >= last_warn_tick + warn_interval)
        	{
        		last_warn_tick = iTickNum;
        		bIsWarning = true;
            	CLagUtils.debug("CLagTileEntityTicker:StartTick warning=true");
        		warnStartTick();
        	}
        }

        lastchunk_dim = Integer.MAX_VALUE; // make sure cur_ticknum check (sum reset) is done for last used chunk as well
        ChunkSkipStartTick();
    }
    
    public void EndTick (int iTickNum) {
        if (bIsProfiling) 
        {
        	if (bIsWarning) warnEndTick();
        	updateWorst();
        	CLagUtils.debug("CLagTileEntityTicker:EndTick profile "+iTickNum+",worst_chunk_time="+worst_chunk_time);
        }
    }


    // ----------------------------- loop
    
    public void runTileEntities(World world, ArrayList<TileEntity> toTick) {
        IChunkProvider chunkProvider = world.getChunkProvider();
        Iterator<TileEntity> iterator = toTick.iterator();
        long end = System.nanoTime();
        long start;
        int dim = world.provider.dimensionId;


        while (iterator.hasNext()) {
            TileEntity tileEntity = iterator.next();

            int x = tileEntity.xCoord;
            int z = tileEntity.zCoord;
            int cx = x >> 4;
            int cz = z >> 4;

            if (isChunkSkippedNow(dim, cx,cz)) continue;

            if (!tileEntity.isInvalid() && tileEntity.hasWorldObj() && chunkProvider.chunkExists(cx, cz)) {
                try {
                    tileEntity.updateEntity();
                } catch (Throwable var6) {
                    CrashReport crashReport = CrashReport.makeCrashReport(var6, "Ticking tile entity");
                    CrashReportCategory crashReportCategory = crashReport.makeCategory("Tile entity being ticked");
                    tileEntity.func_85027_a(crashReportCategory);
                    if (ForgeDummyContainer.removeErroringTileEntities) {
                        FMLLog.severe(crashReport.getCompleteReport());
                        tileEntity.invalidate();
                        world.setBlockToAir(x, tileEntity.yCoord, z);
                    } else {
                        throw new ReportedException(crashReport);
                    }
                }
            }

            if (tileEntity.isInvalid()) {
                iterator.remove();

                if (chunkProvider.chunkExists(tileEntity.xCoord >> 4, tileEntity.zCoord >> 4)) {
                    Chunk chunk = world.getChunkFromChunkCoords(tileEntity.xCoord >> 4, tileEntity.zCoord >> 4);

                    if (chunk != null) {
                        chunk.cleanChunkBlockTileEntity(tileEntity.xCoord & 15, tileEntity.yCoord, tileEntity.zCoord & 15);
                    }
                }
            }

            if (bIsProfiling) {
                start = end;
                end = System.nanoTime();
                if (end > start) profileAddChunkTime(dim, cx, cz, x, tileEntity.yCoord, z, end - start);
            }
        }
    }

    // ---------- ChunkInfo

    static public class ChunkInfoPos
    {
        public int dim;
        public int cx;
        public int cz;
        public ChunkInfoPos (int _dim,int _cx,int _cz)
        {
            dim = _dim;
            cx = _cx;
            cz = _cz;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            ChunkInfoPos o = (ChunkInfoPos) obj;
            return  dim == o.dim && cx == o.cx && cz == o.cz;
        }

        @Override
        public int hashCode() { return dim + 10*cx + 1000*cz; }
    }

    static public class ChunkInfo
    {
        public int ticknum;
        public int iTimeSum;
        public long worst_time;
        public int worst_x;
        public int worst_y;
        public int worst_z;
        public int force_slow_until_tick;
        public ChunkInfoPos pos;
        public ChunkInfo (ChunkInfoPos _pos)
        {
            ticknum = 0;
            iTimeSum = 0;
            worst_time = 0;
            pos = _pos;
            force_slow_until_tick = 0;
        }
    }

    public Map<ChunkInfoPos, ChunkInfo> mChunkInfo = new HashMap<ChunkInfoPos, ChunkInfo>();

    /// get or create
    public ChunkInfo getChunkInfo (int dim,int cx,int cz)
    {
        ChunkInfoPos coord = new ChunkInfoPos(dim,cx,cz);
        ChunkInfo o = mChunkInfo.get(coord);
        if (o == null)
        {
            o = new ChunkInfo(coord);
            mChunkInfo.put(coord, o);
        }
        return o;
    }

    public ChunkInfo getChunkInfoAtPlayer (EntityPlayerMP p)
    {
        int dim = p.dimension;
        int cx = ((int)p.posX) >> 4;
        int cz = ((int)p.posZ) >> 4;
        return getChunkInfo(dim,cx,cz);
    }

    // ---------- internals


    private int lastskip_dim = Integer.MAX_VALUE;
    private int lastskip_cx = Integer.MAX_VALUE;
    private int lastskip_cz = Integer.MAX_VALUE;
    private boolean lastskip_v = false;

    private void ChunkSkipStartTick ()
    {
        // TODO: update for current tick

        // reset cache
        lastskip_dim = Integer.MAX_VALUE;
    }


    private boolean calcIsChunkSkippedNow (int dim,int cx,int cz)
    {
        ChunkInfo o = getChunkInfo(dim,cx,cz);
        int f = getSlowFactor(o);
        if (f <= 1) return false;
        return (cur_ticknum % f) != 0; // return false most of the time
    }

    /// punish laggy chunks by skipping / slowing time in them
    private boolean isChunkSkippedNow (int dim,int cx,int cz)
    {
        if (lastskip_dim == dim && lastskip_cx == cx && lastskip_cz == cz) return lastskip_v;
        lastskip_dim = dim;
        lastskip_cx = cx;
        lastskip_cz = cz;
        lastskip_v = calcIsChunkSkippedNow(dim,cx,cz);
        return lastskip_v;
    }

    private int lastchunk_dim = Integer.MAX_VALUE;
    private int lastchunk_cx = Integer.MAX_VALUE;
    private int lastchunk_cz = Integer.MAX_VALUE;
    private ChunkInfo lastchunkinfo = null;
    public long worst_chunk_time = 0;
    public long worst_chunk_dim = 0;
    public long worst_chunk_cx = 0;
    public long worst_chunk_cz = 0;
    
    private void updateWorst ()
    {
        if (lastchunkinfo != null &&
            worst_chunk_time < lastchunkinfo.iTimeSum)
        {
            worst_chunk_time = lastchunkinfo.iTimeSum;
            worst_chunk_dim = lastchunk_dim;
            worst_chunk_cx = lastchunk_cx;
            worst_chunk_cz = lastchunk_cz;
        }
    }

    // sum consumed time per chunk
    private void profileAddChunkTime(int dim,int cx,int cz, int x, int y, int z, long dt) {
        if (lastchunk_dim != dim || lastchunk_cx != cx || lastchunk_cz != cz)
        {
        	if (lastchunkinfo != null) 
        	{
	            if (worst_chunk_time < lastchunkinfo.iTimeSum)
	            {
	                worst_chunk_time = lastchunkinfo.iTimeSum;
	                worst_chunk_dim = lastchunk_dim;
	                worst_chunk_cx = lastchunk_cx;
	                worst_chunk_cz = lastchunk_cz;
	            }
                
                if (bIsWarning && lastchunkinfo.iTimeSum >= timesum_warn) warnset.add(lastchunkinfo);
            }
            lastchunk_dim = dim;
            lastchunk_cx = cx;
            lastchunk_cz = cz;
            lastchunkinfo = getChunkInfo(dim,cx,cz);

            // reset sum on tick start
            if (lastchunkinfo.ticknum != cur_ticknum)
            {
                lastchunkinfo.ticknum = cur_ticknum;
                lastchunkinfo.iTimeSum = 0;
                lastchunkinfo.worst_time = 0;
            }
        }
        lastchunkinfo.iTimeSum += dt;

        // collect worst tileentity to inform nearby players and help them fix the issue
        if (dt > lastchunkinfo.worst_time)
        {
            lastchunkinfo.worst_time = dt;
            lastchunkinfo.worst_x = x;
            lastchunkinfo.worst_y = y;
            lastchunkinfo.worst_z = z;
        }
    }
}
