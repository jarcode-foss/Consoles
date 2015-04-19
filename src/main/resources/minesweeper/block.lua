-- Written by Levi Webb

Block = {
    x = -1,
    y = -1,
    state = state("COVERED"),
    blockFunction = nil,
    adj = nil,
    bomb = false
}
Block.__index = Block

INST = {}

function Block.new(x, y, blockFunction)
    local self = setmetatable({}, Block);
    self.x = x;
    self.y = y;
    self.blockFunction = blockFunction;
    return self;
end

local function adj(block)
    return {
        block.blockFunction(block.x + 1, block.y),
        block.blockFunction(block.x - 1, block.y),
        block.blockFunction(block.x, block.y + 1),
        block.blockFunction(block.x, block.y - 1),
        block.blockFunction(block.x + 1, block.y + 1),
        block.blockFunction(block.x - 1, block.y - 1),
        block.blockFunction(block.x - 1, block.y + 1),
        block.blockFunction(block.x + 1, block.y - 1),
    }
end

function Block:countAdj()
    local adj = adj(self)
    local c = 0;
    for i = 1,#adj do
        if (adj[i]:isBomb()) then
            c = c + 1
        end
    end

    if self:isBomb then
        c = c + 1
    end

    return c;
end

function Block:reveal()
    local adj = adj(self)
    for i = 1,#adj do
        if (adj[i]:countAdj() == 0) then
            adj[i]:setState(state("REVEALED"))
            adj[i]:reveal()
        else
            adj[i]:setState(state("SHOWING"))
        end
    end
end

function Block:isBomb()
    return self.bomb;
end

function Block:onClick()
    if (self.bomb) then return true else
        self.reveal()
        return false
    end
end

function Block:setState(state)
    self.state = state;
end

function Block:getState()
    return self.state;
end

function Block:getX()
    return self.x;
end

function Block:getY()
    return self.y;
end

local function state(name)
    if (name == "COVERED") then return 1 elseif
    (name == "SHOWING") then return 2 elseif
    (name == "REVEALED") then return 3 else
    return -1 end
end

INST.Block = Block;

return INST