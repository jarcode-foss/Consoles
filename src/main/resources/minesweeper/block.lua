-- Written by Jarcode

-- converts strings into integral states
local function state(name)
    if (name == "COVERED") then return 1 elseif
    (name == "REVEALED") then return 3 else
        return -1 end
end

-- our object information
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
    self.blockFunction = blockFunction
    return self;
end

-- counts the adjacent squares for bombs
function Block:countAdj()
    local adj = self:adjacent()
    local c = 0;
    for i = 1,#adj do
        if (adj[i]:isBomb()) then
            c = c + 1
        end
    end

    if self.bomb then
        c = c + 1
    end

    return c;
end

-- recursive method to reveal tiles
function Block:reveal()
    local adj = self:adjacent()
    for i = 1,#adj do
        if (adj[i]:countAdj() == 0 and adj[i]:getState() == state("COVERED")) then
            adj[i]:setState(state("REVEALED"))
            adj[i]:reveal()
        elseif adj[i]:isBomb() == false then
            adj[i]:setState(state("REVEALED"))
        end
    end
end

function Block:setBomb(bomb)
    self.bomb = bomb
end

function Block:isBomb()
    return self.bomb;
end

-- method called when clicked
function Block:onClick()
    if (self.state ~= state("COVERED")) then return false end
    if (self.bomb) then return true else
        self:setState(state("REVEALED"))
        self:reveal()
        return false
    end
end

-- finds all adjacent tiles
function Block:adjacent()

    local mods = {}
    mods[1] = {self.x + 1, self.y}
    mods[2] = {self.x - 1, self.y}
    mods[3] = {self.x, self.y + 1}
    mods[4] = {self.x, self.y - 1}
    mods[5] = {self.x + 1, self.y + 1}
    mods[6] = {self.x - 1, self.y - 1}
    mods[7] = {self.x - 1, self.y + 1}
    mods[8] = {self.x + 1, self.y - 1}

    local blocks = {}

    local at = 1;

    for i = 1,#mods do
        local x = mods[i][1]
        local y = mods[i][2]
        if (x >= 1 and x <= GAME_WIDTH and y >= 1 and y <= GAME_HEIGHT) then
            blocks[at] = self.blockFunction(x, y)
            at = at + 1
        end
    end

    return blocks;
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

INST.Block = Block
INST.state = state

return INST