package enemeez.simplefarming.block;

import java.util.Random;

import enemeez.simplefarming.init.ModRecipes;
import enemeez.simplefarming.tileentity.BrewingBarrelTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BrewingBarrelBlock extends ContainerBlock {
	public static final IntegerProperty PROGRESS = IntegerProperty.create("progress", 0, 3);
	public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 0, 1);

	public BrewingBarrelBlock(Properties properties) {
		super(properties);
		this.setDefaultState(
				this.stateContainer.getBaseState().with(LAYERS, Integer.valueOf(0)).with(PROGRESS, Integer.valueOf(0)));
	}

	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(IBlockReader worldIn) {
		return new BrewingBarrelTileEntity();
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(LAYERS, PROGRESS);
	}

	public int getProgress(BlockState state) {
		return state.get(PROGRESS);
	}

	public int getLayers(BlockState state) {
		return state.get(LAYERS);
	}

	private boolean readyToFerment(BlockState state) {
		if (getLayers(state) == 1)
			return true;
		return false;
	}

	public void reset(BlockState state, World worldIn, BlockPos pos) {
		worldIn.setBlockState(pos,
				this.getDefaultState().with(LAYERS, Integer.valueOf(0)).with(PROGRESS, Integer.valueOf(0)));
	}

	@Override
	public ActionResultType func_225533_a_(BlockState state, World worldIn, BlockPos pos, PlayerEntity player,
			Hand handIn, BlockRayTraceResult hit) {
		TileEntity tileentity = worldIn.getTileEntity(pos);
		if (tileentity instanceof BrewingBarrelTileEntity) {
			if (player.func_225608_bj_()) {
				if (getLayers(state) == 1 && getProgress(state) == 0) {
					this.dropItem(worldIn, pos);
					state = state.with(LAYERS, Integer.valueOf(0));
					worldIn.setBlockState(pos, state);
					return ActionResultType.SUCCESS;
				}
			}
			if (isAlcoholIngredient(worldIn, player.getHeldItemMainhand())
					&& ((BrewingBarrelTileEntity) tileentity).getCapacity() == 0) {
				this.insertItem(worldIn, pos, state, player.getHeldItemMainhand().getItem());
				player.getHeldItemMainhand().shrink(1);
				state = state.with(LAYERS, Integer.valueOf(1));
				worldIn.setBlockState(pos, state);
				return ActionResultType.SUCCESS;
			}
			if (player.getHeldItemMainhand().getItem() == Items.GLASS_BOTTLE && this.getProgress(state) == 3) {
				player.addItemStackToInventory(getProduct(worldIn, ((BrewingBarrelTileEntity) tileentity).getItem()));
				((BrewingBarrelTileEntity) tileentity).clear();
				worldIn.playSound((PlayerEntity) null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F,
						0.8F + worldIn.rand.nextFloat() * 0.4F);
				player.getHeldItemMainhand().shrink(1);
				this.reset(state, worldIn, pos);
				return ActionResultType.SUCCESS;
			}
		}
		return ActionResultType.PASS;

	}

	public void insertItem(IWorld worldIn, BlockPos pos, BlockState state, Item item) {
		TileEntity tileentity = worldIn.getTileEntity(pos);
		if (tileentity instanceof BrewingBarrelTileEntity) {
			if (((BrewingBarrelTileEntity) tileentity).getCapacity() == 0) {
				((BrewingBarrelTileEntity) tileentity).setItem(item);
				worldIn.setBlockState(pos, state.with(LAYERS, Integer.valueOf(1)), 2);
			}
		}
	}

	private void dropItem(World worldIn, BlockPos pos) {
		if (!worldIn.isRemote) {
			TileEntity tileentity = worldIn.getTileEntity(pos);
			if (tileentity instanceof BrewingBarrelTileEntity) {
				BrewingBarrelTileEntity tile = (BrewingBarrelTileEntity) tileentity;
				Item item = tile.getItem();
				if (tile.getCapacity() > 0) {
					worldIn.playEvent(1010, pos, 0);
					tile.clear();
					double d0 = (double) (worldIn.rand.nextFloat() * 0.7F) + (double) 0.15F;
					double d1 = (double) (worldIn.rand.nextFloat() * 0.7F) + (double) 0.060000002F + 0.6D;
					double d2 = (double) (worldIn.rand.nextFloat() * 0.7F) + (double) 0.15F;
					ItemStack itemstack1 = new ItemStack(item);
					ItemEntity itementity = new ItemEntity(worldIn, (double) pos.getX() + d0, (double) pos.getY() + d1,
							(double) pos.getZ() + d2, itemstack1);
					itementity.setDefaultPickupDelay();
					worldIn.addEntity(itementity);
				}
			}
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			this.dropItem(worldIn, pos);
			super.onReplaced(state, worldIn, pos, newState, isMoving);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void func_225534_a_(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
		if (this.readyToFerment(state)) {
			super.func_225534_a_(state, worldIn, pos, random);
			int i = state.get(PROGRESS);
			if (i < 3 && random.nextInt(5) == 0) {
				worldIn.setBlockState(pos,
						this.getDefaultState().with(LAYERS, Integer.valueOf(1)).with(PROGRESS, Integer.valueOf(i + 1)),
						2);
			}
		}

	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
		if (this.getProgress(stateIn) < 3 && this.getProgress(stateIn) > 0) {
			double d0 = (double) ((float) pos.getX() + (Double) Math.random());
			double d1 = (double) ((float) pos.getY() + (Double) Math.random());
			double d2 = (double) ((float) pos.getZ() + (Double) Math.random());
			worldIn.addParticle(ParticleTypes.AMBIENT_ENTITY_EFFECT, d0, d1 + 1, d2, 0.0D, 0.0D, 0.0D);
		}
	}

	public ItemStack getProduct(World worldIn, Item itemIn) {
		return worldIn.getRecipeManager().getRecipe(ModRecipes.BREWING_BARREL_RECIPE_TYPE, new Inventory(new ItemStack(itemIn)), worldIn)
				.map(recipe -> recipe.getCraftingResult(null))
				.orElse(ItemStack.EMPTY);
	}

	public boolean isAlcoholIngredient(World worldIn, ItemStack itemstackIn) {
		return worldIn.getRecipeManager().getRecipe(ModRecipes.BREWING_BARREL_RECIPE_TYPE, new Inventory(itemstackIn), worldIn).isPresent();
	}

}
