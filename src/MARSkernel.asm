# This kernel is designed to replicate the behaviour of the original MARS simulator as faithfully as possible
#
# Author: Project2100



.ktext

j main

.ktext 0x00000180

bne $v0, SYS_1

# SYSCALL 0:

eret

SYS_1:
subi $v0, $v0, 1
bne $v0, SYS_2

# SYSCALL 1:

ori $v0, $zero, 1
eret

SYS_2:
subi $v0, $v0, 1
bne $v0, SYS_3

# SYSCALL 2:

ori $v0, $zero, 2
eret

SYS_3:
subi $v0, $v0, 1
bne $v0, SYS_4

# SYSCALL 3:

ori $v0, $zero, 3
eret

SYS_4:
subi $v0, $v0, 1
bne $v0, SYS_5

# SYSCALL 4:

ori $v0, $zero, 4
eret

SYS_5:
subi $v0, $v0, 1
bne $v0, SYS_6

# SYSCALL 5:

ori $v0, $zero, 5
eret

SYS_6:
subi $v0, $v0, 1
bne $v0, SYS_7

# SYSCALL 6:

ori $v0, $zero, 6
eret

SYS_7:
subi $v0, $v0, 1
bne $v0, SYS_8

# SYSCALL 7:

ori $v0, $zero, 7
eret

SYS_8:
subi $v0, $v0, 1
bne $v0, SYS_9

# SYSCALL 8:

ori $v0, $zero, 8
eret

SYS_9:
subi $v0, $v0, 1
bne $v0, SYS_10

# SYSCALL 9:

ori $v0, $zero, 9
eret

SYS_10:
subi $v0, $v0, 1
bne $v0, SYS_11

# SYSCALL 10:

ori $v0, $zero, 10  # Unreachable code here!
eret                # Unreachable code here!

# Current Idea: OS/Kernel has its own MMIO addresses: one for display, one for input (actually the same, it's the console - dedicate a whole 4KiB block?), one for device power-down



.text
main:
